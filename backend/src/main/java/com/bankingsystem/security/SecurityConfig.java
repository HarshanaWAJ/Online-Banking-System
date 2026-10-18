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
 * Spring Security configuration:
 * - Stateless session policy with JWT in httpOnly cookie
 * - CSRF enabled using CookieCsrfTokenRepository (XSRF-TOKEN / X-XSRF-TOKEN)
 * - Strict CORS configuration
 * - Method-level security (@EnableMethodSecurity)
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

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(appProperties.getBcrypt().strength());
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CORS configuration
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // CSRF protection using cookie repo with httpOnly=false so Axios can read & send X-XSRF-TOKEN header
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

        // Stateless sessions
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Return 401 instead of redirecting to a login page
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        )

        // Route authorizations
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

            // Pre-flight OPTIONS
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // Role-based protections
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/staff/**").hasRole("STAFF")

            // Any other /api/ endpoint requires authentication
            .requestMatchers("/api/**").authenticated()

            .anyRequest().permitAll()
        )

        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

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
        csrfToken.getToken();
      }
      filterChain.doFilter(request, response);
    }
  }

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
