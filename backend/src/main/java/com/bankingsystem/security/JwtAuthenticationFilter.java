package com.bankingsystem.security;

import com.bankingsystem.repository.AppUserRepository;
import com.bankingsystem.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Authentication Filter.
 *
 * Intercepts every incoming HTTP request prior to reaching Spring Security's authorization filters.
 *
 * Security Architecture & Responsibilities:
 * 1. OncePerRequest Guarantee:
 *    Inherits from {@link OncePerRequestFilter} ensuring deterministic, single execution per request dispatch.
 *
 * 2. Cookie Extraction:
 *    Extracts the signed JWT from the secure `httpOnly` session cookie via {@link CookieService}.
 *    This ensures client-side JavaScript has zero access to the raw token (neutralizing XSS token theft).
 *
 * 3. Token Parsing & Claims Extraction:
 *    Parses cryptographic claims using {@link JwtService}:
 *    - `username` (Subject): The user's registered email address.
 *    - `jti` (JWT ID): A unique UUID assigned to this specific login session.
 *
 * 4. Stateful Session Verification (Database Blacklist/Revocation Check):
 *    Unlike pure stateless JWTs that cannot be revoked until natural expiry,
 *    this filter queries {@link UserSessionRepository} with the `jti`.
 *    If the session was explicitly revoked (e.g. user clicked Logout, password was changed, or account disabled),
 *    the request is immediately rejected even if the cryptographic signature is mathematically valid.
 *
 * 5. Security Context Establishment:
 *    When valid, loads {@link org.springframework.security.core.userdetails.UserDetails}, builds a
 *    {@link UsernamePasswordAuthenticationToken} with granted roles/authorities, and attaches it to the
 *    thread-local {@link SecurityContextHolder}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtService jwtService;
  private final CookieService cookieService;
  private final AppUserDetailsService userDetailsService;
  private final UserSessionRepository sessionRepository;
  private final AppUserRepository userRepository;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      CookieService cookieService,
      AppUserDetailsService userDetailsService,
      UserSessionRepository sessionRepository,
      AppUserRepository userRepository) {
    this.jwtService = jwtService;
    this.cookieService = cookieService;
    this.userDetailsService = userDetailsService;
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    // Step 1: Look for JWT cookie on the request
    cookieService.extractToken(request).ifPresentOrElse(token -> {
      try {
        // Step 2: Extract subject and session ID claims
        String username = jwtService.extractUsername(token);
        String jti = jwtService.extractJti(token);

        // Step 3: Verify whether security context is already initialized
        if (username != null && jti != null && SecurityContextHolder.getContext().getAuthentication() == null) {

          // Step 4: Verify the active session record in the database
          var sessionOpt = sessionRepository.findByJti(jti);
          if (sessionOpt.isPresent()) {
            var session = sessionOpt.get();

            // Ensure session is not expired and user account is active
            if (session.getExpiresAt().isAfter(Instant.now()) && session.getUser().isActive()) {
              // Load user details with assigned authorities/roles
              var userDetails = userDetailsService.loadUserByUsername(username);

              // Construct Spring Security authentication token
              var authToken = new UsernamePasswordAuthenticationToken(
                  userDetails,
                  null, // Credentials omitted as token has already been validated
                  userDetails.getAuthorities());

              authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

              // Attach authenticated principal to the thread-local SecurityContext
              SecurityContextHolder.getContext().setAuthentication(authToken);
              log.debug("Successfully authenticated user '{}' for path '{}'", username, request.getRequestURI());
            } else {
              log.warn("Session expired or user inactive for jti '{}'", jti);
            }
          } else {
            log.warn("No active UserSession record found for jti '{}' (likely logged out)", jti);
          }
        }
      } catch (Exception ex) {
        log.error("Authentication filter failed to process JWT: {}", ex.getMessage());
      }
    }, () -> {
      log.debug("No session cookie present on request to '{}'", request.getRequestURI());
    });

    // Proceed down the servlet filter chain
    filterChain.doFilter(request, response);
  }
}
