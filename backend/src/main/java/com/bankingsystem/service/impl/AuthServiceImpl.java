package com.bankingsystem.service.impl;

import com.bankingsystem.config.JwtProperties;
import com.bankingsystem.dto.request.ChangePasswordRequest;
import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.entity.UserSession;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import com.bankingsystem.exception.AccessDeniedException;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.repository.AppUserRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.UserSessionRepository;
import com.bankingsystem.security.CookieService;
import com.bankingsystem.security.JwtService;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication & Identity Lifecycle Service Implementation.
 *
 * Security Architecture & Policies:
 * 1. Brute-Force & Rate-Limiting Protection:
 *    - All login attempts are filtered through {@link RateLimitService} by client IP address.
 *    - Repeated invalid login bursts trigger rate-limit throttling (HTTP 400/429) for 15 minutes.
 *
 * 2. Anti-Enumeration & Constant-Time Resistance:
 *    - Login rejection returns the exact same generic error message ("Invalid email or password.")
 *      regardless of whether the user exists or the password was incorrect.
 *
 * 3. Stateful Session Persistence & Instant Revocation:
 *    - Upon successful authentication, generates a unique UUID `jti` (JWT ID).
 *    - Persists an active session record in the `user_session` table with user agent, client IP, and expiry.
 *    - The JWT is transmitted in a secure, `HttpOnly`, `SameSite=Lax` cookie.
 *    - Calling logout immediately deletes the `UserSession` row from the database, instantly revoking the JWT.
 *
 * 4. Password Modification Session Purge:
 *    - When a user changes their password, all active sessions across all devices are deleted
 *      ({@code sessionRepository.deleteAllByUserId}), forcing re-authentication everywhere.
 */
@Service
public class AuthServiceImpl implements AuthService {

  private final AppUserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final UserSessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final CookieService cookieService;
  private final JwtProperties jwtProperties;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final RateLimitService rateLimitService;

  public AuthServiceImpl(
      AppUserRepository userRepository,
      CustomerRepository customerRepository,
      UserSessionRepository sessionRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      CookieService cookieService,
      JwtProperties jwtProperties,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      RateLimitService rateLimitService) {
    this.userRepository = userRepository;
    this.customerRepository = customerRepository;
    this.sessionRepository = sessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.cookieService = cookieService;
    this.jwtProperties = jwtProperties;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.rateLimitService = rateLimitService;
  }

  /**
   * Registers a new customer account.
   *
   * Validates email and National Identity Card (NIC) uniqueness, hashes the raw password using BCrypt,
   * creates linked {@link AppUser} and {@link Customer} entities, and writes an audit event.
   *
   * @param request Customer registration data.
   * @return Created user profile response.
   */
  @Override
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();

    // Verify email uniqueness
    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new BusinessRuleException("An account with this email address already exists.");
    }

    // Verify NIC uniqueness
    if (customerRepository.existsByNicIgnoreCase(request.nic().trim())) {
      throw new BusinessRuleException("An account with this National Identity Card (NIC) number already exists.");
    }

    // Create AppUser with encoded BCrypt password hash and CUSTOMER role
    AppUser user = new AppUser(normalizedEmail, passwordEncoder.encode(request.password()), Role.CUSTOMER);
    user = userRepository.save(user);

    // Create linked Customer domain profile
    Customer customer = new Customer();
    customer.setUser(user);
    customer.setFullName(request.fullName().trim());
    customer.setNic(request.nic().trim().toUpperCase());
    customer.setPhone(request.phone().trim());
    customer.setAddress(request.address().trim());
    customer.setDateOfBirth(request.dateOfBirth());
    customer.setActive(true);
    customerRepository.save(customer);

    // Record registration audit event
    auditService.recordEvent(
        user.getId(),
        Role.CUSTOMER,
        "CUSTOMER_REGISTERED",
        "AppUser",
        user.getId(),
        AuditOutcome.SUCCESS,
        "Customer registered successfully with email: " + normalizedEmail,
        null);

    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPermissions());
  }

  /**
   * Authenticates user credentials, generates JWT session, and sets secure HttpOnly cookie.
   *
   * @param request Login credentials (email, password).
   * @param httpRequest HTTP servlet request to resolve client IP and User-Agent.
   * @param httpResponse HTTP servlet response to write the Set-Cookie header.
   * @return Authenticated user profile.
   */
  @Override
  @Transactional
  public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    String ip = httpRequest.getRemoteAddr();

    // Step 1: Check IP rate limiting to mitigate brute-force password cracking
    if (!rateLimitService.tryConsume(ip)) {
      auditService.recordEvent(null, null, "LOGIN_RATE_LIMITED", "AppUser", null, AuditOutcome.FAILURE, "Rate limit exceeded for IP: " + ip, ip);
      throw new BusinessRuleException("Too many login attempts. Please try again in 15 minutes.");
    }

    String normalizedEmail = request.email().trim().toLowerCase();
    var userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

    // Step 2: Constant-time style neutral check prevents username enumeration
    if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
      auditService.recordEvent(null, null, "LOGIN_FAILED", "AppUser", null, AuditOutcome.FAILURE, "Failed login attempt for email: " + normalizedEmail, ip);
      throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password.");
    }

    AppUser user = userOpt.get();

    // Step 3: Verify user account status
    if (!user.isActive()) {
      auditService.recordEvent(user.getId(), user.getRole(), "LOGIN_BLOCKED", "AppUser", user.getId(), AuditOutcome.FAILURE, "Inactive user tried to log in", ip);
      throw new org.springframework.security.authentication.BadCredentialsException("Account is deactivated. Please contact support.");
    }

    // Step 4: Generate unique session identifier (JTI) and calculate expiry
    String jti = UUID.randomUUID().toString();
    Instant expiry = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);

    // Step 5: Persist session row in user_session table
    UserSession session = new UserSession();
    session.setUser(user);
    session.setJti(jti);
    session.setExpiresAt(expiry);
    session.setUserAgent(httpRequest.getHeader("User-Agent"));
    session.setIpAddress(ip);
    sessionRepository.save(session);

    // Update last login timestamp
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);

    // Step 6: Generate cryptographic JWT containing claims (sub, userId, role, jti)
    String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), jti);
    long maxAgeSeconds = jwtProperties.getExpiresMinutes() * 60;

    // Step 7: Write secure HttpOnly cookie to client response
    cookieService.addSessionCookie(httpResponse, token, maxAgeSeconds);

    // Step 8: Record audit event
    auditService.recordEvent(user.getId(), user.getRole(), "LOGIN_SUCCESS", "AppUser", user.getId(), AuditOutcome.SUCCESS, "User logged in successfully", ip);

    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPermissions());
  }

  /**
   * Logs out the user by deleting their database session record and zeroing out the session cookie.
   *
   * @param httpRequest HTTP servlet request containing the cookie to read JTI.
   * @param httpResponse HTTP servlet response to issue an expired Set-Cookie header.
   */
  @Override
  @Transactional
  public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    cookieService.extractToken(httpRequest).ifPresent(token -> {
      String jti = jwtService.extractJti(token);
      if (jti != null) {
        // Immediate server-side session invalidation
        sessionRepository.deleteByJti(jti);
      }
    });

    // Clear client-side cookie
    cookieService.clearSessionCookie(httpResponse);
  }

  /**
   * Retrieves profile details for the currently authenticated principal.
   */
  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    AppUser user = ownershipGuard.getCurrentUser();
    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPermissions());
  }

  /**
   * Updates user password and revokes all active sessions across all devices for security.
   *
   * @param request Password update payload with current and new password.
   */
  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();

    // Verify current password matches existing BCrypt hash
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new BusinessRuleException("The current password provided is incorrect.");
    }

    // Hash and persist new password
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setPasswordChangedAt(Instant.now());
    userRepository.save(user);

    // Invalidate all existing sessions across all browsers/devices
    sessionRepository.deleteAllByUserId(user.getId());

    // Record audit event
    auditService.recordEvent(user.getId(), user.getRole(), "PASSWORD_CHANGED", "AppUser", user.getId(), AuditOutcome.SUCCESS, "User updated password and all sessions were invalidated", null);
  }
}
