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
 * Filter intercepting incoming requests to extract JWT from httpOnly cookie,
 * verify active session state in the database, and establish SecurityContext.
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

    cookieService.extractToken(request).ifPresentOrElse(token -> {
      try {
        String username = jwtService.extractUsername(token);
        String jti = jwtService.extractJti(token);

        if (username != null && jti != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          var sessionOpt = sessionRepository.findByJti(jti);
          if (sessionOpt.isPresent()) {
            var session = sessionOpt.get();
            if (session.getExpiresAt().isAfter(Instant.now()) && session.getUser().isActive()) {
              var userDetails = userDetailsService.loadUserByUsername(username);
              var authToken = new UsernamePasswordAuthenticationToken(
                  userDetails,
                  null,
                  userDetails.getAuthorities());
              authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(authToken);
              log.info("Successfully authenticated user '{}' for path '{}'", username, request.getRequestURI());
            } else {
              log.warn("Session expired or user inactive for jti '{}'", jti);
            }
          } else {
            log.warn("No UserSession row found in database for jti '{}'", jti);
          }
        }
      } catch (Exception ex) {
        log.error("Authentication via JWT failed: {}", ex.getMessage(), ex);
      }
    }, () -> {
      log.debug("No session cookie present on request to '{}'", request.getRequestURI());
    });

    filterChain.doFilter(request, response);
  }
}
