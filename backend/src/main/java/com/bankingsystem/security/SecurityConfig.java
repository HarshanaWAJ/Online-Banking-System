package com.bankingsystem.security;

import com.bankingsystem.config.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 6 Central Configuration.
 *
 * Core Security Architecture:
 * 1. Stateless Session Management:
 *    - `SessionCreationPolicy.STATELESS` ensures Spring Security never creates HTTP sessions (no `JSESSIONID`).
 *    - Authentication is driven strictly through signed, HTTP-only JWT cookies verified on each request.
 *
 * 2. Single-Page Application (SPA) CSRF Mitigation:
 *    - Employs double-submit CSRF cookie pattern using {@link CookieCsrfTokenRepository#withHttpOnlyFalse()}.
 *    - Sets cookie `XSRF-TOKEN` which JavaScript (Axios) reads and mirrors in header `X-XSRF-TOKEN`.
 *    - Integrates {@link CsrfCookieFilter} to force evaluation and persistence of the CSRF token on every request.
 *
 * 3. Cross-Origin Resource Sharing (CORS):
 *    - Restricts incoming browser requests strictly to the configured frontend origin (e.g. `http://localhost:5173`).
 *    - Permits standard HTTP verbs (GET, POST, PUT, PATCH, DELETE, OPTIONS).
 *    - Enables `allowCredentials(true)` so the browser transmits cookies during cross-origin fetch/XHR.
 *
 * 4. Exception Handling:
 *    - Replaces default Spring Security redirect-to-login-page behavior with {@link HttpStatusEntryPoint}
 *      returning HTTP 401 Unauthorized directly for REST consumers.
 *
 * 5. Role-Based Access Control (RBAC):
 *    - Public access granted to authentication routes, health endpoints, and OpenAPI docs.
 *    - `/api/admin/**` reserved strictly for `ROLE_ADMIN`.
 *    - `/api/staff/**` reserved strictly for `ROLE_STAFF`.
 *    - Method-level security enabled via {@link EnableMethodSecurity}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final AppProperties appProperties;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AppProperties appProperties) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.appProperties = appProperties;
  }

  /**
   * BCrypt password encoder configured with adaptive work factor strength.
   * Protects stored credentials against offline brute-force and rainbow table attacks.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(appProperties.getBcrypt().strength());
  }

  /**
   * Exposes Spring Security's AuthenticationManager bean for programmatic authentication.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Configures the main HTTP security filter chain and request matchers.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. CORS Filter Configuration
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 2. CSRF Protection for SPA
        .csrf(csrf -> {
          org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler =
              new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
          requestHandler.setCsrfRequestAttributeName(null);
          csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
              .csrfTokenRequestHandler(requestHandler)
              .ignoringRequestMatchers(
                  "/api/auth/login",
                  "/api/auth/register",
                  "/actuator/**",
                  "/swagger-ui/**",
                  "/v3/api-docs/**"
              );
        })

        // 3. Stateless Session Policy
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 4. Return HTTP 401 instead of redirecting to login page
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        )

        // 5. Route-Level Authorization Matchers
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

            // Pre-flight HTTP OPTIONS requests for CORS negotiation
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // Administrative role restrictions
            .requestMatchers("/api/admin/**").hasRole("ADMIN")

            // Bank employee / Teller role restrictions
            .requestMatchers("/api/staff/**").hasRole("STAFF")

            // Any other banking API endpoint requires an authenticated principal
            .requestMatchers("/api/**").authenticated()

            .anyRequest().permitAll()
        )

        // 6. Custom Filter Ordering
        // Place JWT filter before standard UsernamePasswordAuthenticationFilter
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        // Add CSRF cookie filter after authentication filter so XSRF-TOKEN is refreshed
        .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Custom servlet filter that explicitly forces generation of the CSRF token.
   * In Spring Security 6, CSRF tokens are deferred by default; calling `csrfToken.getToken()`
   * ensures the token repository writes the `XSRF-TOKEN` cookie to the HTTP response.
   */
  private static class CsrfCookieFilter extends org.springframework.web.filter.OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        jakarta.servlet.FilterChain filterChain)
        throws jakarta.servlet.ServletException, java.io.IOException {
      org.springframework.security.web.csrf.CsrfToken csrfToken =
          (org.springframework.security.web.csrf.CsrfToken)
              request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
      if (csrfToken != null) {
        csrfToken.getToken(); // Forces evaluation and cookie persistence
      }
      filterChain.doFilter(request, response);
    }
  }

  /**
   * Constructs CORS configuration source defining allowed origins, headers, and exposed response headers.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(appProperties.getCors().allowedOrigin()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Set-Cookie", "X-XSRF-TOKEN"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
