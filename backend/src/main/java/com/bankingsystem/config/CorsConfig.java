package com.bankingsystem.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC CORS configuration.
 *
 * <p>Reads the trusted frontend origin from {@link AppProperties} and registers a global CORS
 * mapping that permits the React/Vue front-end to call all API endpoints with credentials
 * (cookies). The {@code Access-Control-Max-Age} header is set to 3600 seconds so browsers cache
 * the preflight response for one hour.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final AppProperties appProperties;

  /**
   * Constructs a {@code CorsConfig} with the application-level properties.
   *
   * @param appProperties the application properties providing the allowed CORS origin
   */
  public CorsConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  /**
   * Registers global CORS mappings for all API endpoints.
   *
   * @param registry the {@link CorsRegistry} to configure
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String allowedOrigin = appProperties.getCors().allowedOrigin();
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigin)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*", "X-XSRF-TOKEN", "Content-Type", "Authorization")
        .allowCredentials(true)
        .maxAge(3600L);
  }
}
