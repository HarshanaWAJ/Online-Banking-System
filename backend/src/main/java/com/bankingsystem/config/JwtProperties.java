package com.bankingsystem.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed, validated configuration properties for JWT token generation and verification.
 *
 * <p>Bound from the {@code app.jwt.*} namespace in {@code application.yml}. The application will
 * fail to start if the secret is shorter than 32 characters or if expiry is outside the allowed
 * range, preventing insecure JWT configuration from reaching production.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

  private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);

  /**
   * HMAC-SHA signing secret. Must be at least 32 characters long to satisfy JJWT's minimum key
   * length requirement for HS256.
   */
  @NotBlank
  @Size(min = 32, message = "JWT secret must be at least 32 characters to ensure adequate security")
  private String secret;

  /**
   * Token time-to-live in minutes. Values below 5 min are too short for usability; above 1440 min
   * (24 h) increase exposure if a token is compromised.
   */
  @Min(5)
  @Max(1440)
  private long expiresMinutes;

  /**
   * Lifecycle callback that logs a confirmation message (without the actual secret value) once the
   * properties are bound and validated.
   */
  @PostConstruct
  public void logLoaded() {
    log.info(
        "JWT configuration loaded successfully — expiresMinutes={}, secret length={}",
        expiresMinutes,
        secret == null ? 0 : secret.length());
  }

  /**
   * Returns the HMAC signing secret.
   *
   * @return the secret string
   */
  public String getSecret() {
    return secret;
  }

  /**
   * Sets the HMAC signing secret (called by Spring binding).
   *
   * @param secret the secret string
   */
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * Returns the token expiry duration in minutes.
   *
   * @return expiry in minutes
   */
  public long getExpiresMinutes() {
    return expiresMinutes;
  }

  /**
   * Sets the token expiry duration in minutes (called by Spring binding).
   *
   * @param expiresMinutes expiry in minutes
   */
  public void setExpiresMinutes(long expiresMinutes) {
    this.expiresMinutes = expiresMinutes;
  }
}
