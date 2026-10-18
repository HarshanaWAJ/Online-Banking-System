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
 * Authentication and session service implementation.
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

  @Override
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();

    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new BusinessRuleException("An account with this email address already exists.");
    }
    if (customerRepository.existsByNicIgnoreCase(request.nic().trim())) {
      throw new BusinessRuleException("An account with this National Identity Card (NIC) number already exists.");
    }

    AppUser user = new AppUser(normalizedEmail, passwordEncoder.encode(request.password()), Role.CUSTOMER);
    user = userRepository.save(user);

    Customer customer = new Customer();
    customer.setUser(user);
    customer.setFullName(request.fullName().trim());
    customer.setNic(request.nic().trim().toUpperCase());
    customer.setPhone(request.phone().trim());
    customer.setAddress(request.address().trim());
    customer.setDateOfBirth(request.dateOfBirth());
    customer.setActive(true);
    customerRepository.save(customer);

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

  @Override
  @Transactional
  public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    String ip = httpRequest.getRemoteAddr();

    // Check login rate limiting per IP
    if (!rateLimitService.tryConsume(ip)) {
      auditService.recordEvent(null, null, "LOGIN_RATE_LIMITED", "AppUser", null, AuditOutcome.FAILURE, "Rate limit exceeded for IP: " + ip, ip);
      throw new BusinessRuleException("Too many login attempts. Please try again in 15 minutes.");
    }

    String normalizedEmail = request.email().trim().toLowerCase();
    var userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

    // Constant-time style neutral error message prevents user enumeration
    if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
      auditService.recordEvent(null, null, "LOGIN_FAILED", "AppUser", null, AuditOutcome.FAILURE, "Failed login attempt for email: " + normalizedEmail, ip);
      throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password.");
    }

    AppUser user = userOpt.get();
    if (!user.isActive()) {
      auditService.recordEvent(user.getId(), user.getRole(), "LOGIN_BLOCKED", "AppUser", user.getId(), AuditOutcome.FAILURE, "Inactive user tried to log in", ip);
      throw new org.springframework.security.authentication.BadCredentialsException("Account is deactivated. Please contact support.");
    }

    // Generate unique session identifier JTI
    String jti = UUID.randomUUID().toString();
    Instant expiry = Instant.now().plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);

    UserSession session = new UserSession();
    session.setUser(user);
    session.setJti(jti);
    session.setExpiresAt(expiry);
    session.setUserAgent(httpRequest.getHeader("User-Agent"));
    session.setIpAddress(ip);
    sessionRepository.save(session);

    user.setLastLoginAt(Instant.now());
    userRepository.save(user);

    String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), jti);
    long maxAgeSeconds = jwtProperties.getExpiresMinutes() * 60;
    cookieService.addSessionCookie(httpResponse, token, maxAgeSeconds);

    auditService.recordEvent(user.getId(), user.getRole(), "LOGIN_SUCCESS", "AppUser", user.getId(), AuditOutcome.SUCCESS, "User logged in successfully", ip);

    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPermissions());
  }

  @Override
  @Transactional
  public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    cookieService.extractToken(httpRequest).ifPresent(token -> {
      String jti = jwtService.extractJti(token);
      if (jti != null) {
        sessionRepository.deleteByJti(jti);
      }
    });

    cookieService.clearSessionCookie(httpResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    AppUser user = ownershipGuard.getCurrentUser();
    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPermissions());
  }

  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new BusinessRuleException("The current password provided is incorrect.");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setPasswordChangedAt(Instant.now());
    userRepository.save(user);

    // Invalidate all existing sessions on password change
    sessionRepository.deleteAllByUserId(user.getId());

    auditService.recordEvent(user.getId(), user.getRole(), "PASSWORD_CHANGED", "AppUser", user.getId(), AuditOutcome.SUCCESS, "User updated password and all sessions were invalidated", null);
  }
}
