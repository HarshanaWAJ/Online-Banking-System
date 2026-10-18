package com.bankingsystem.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed, validated configuration properties bound from the {@code app.*} namespace in
 * {@code application.yml}.
 *
 * <p>All nested sections are represented as Java records, making the configuration immutable and
 * easy to inject via constructor injection throughout the application.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

  @Valid private Cors cors = new Cors("");

  @Valid private Cookie cookie = new Cookie("", false);

  @Valid private Bcrypt bcrypt = new Bcrypt(12);

  @Valid private RateLimit rateLimit = new RateLimit(5, 15);

  @Valid private Db db = new Db(3000L);

  @Valid private Seed seed = new Seed(false, "", "", "", "", "");

  /**
   * Returns the CORS configuration.
   *
   * @return the {@link Cors} record
   */
  public Cors getCors() {
    return cors;
  }

  /**
   * Sets the CORS configuration (called by Spring binding).
   *
   * @param cors the CORS configuration to set
   */
  public void setCors(Cors cors) {
    this.cors = cors;
  }

  /**
   * Returns the cookie configuration.
   *
   * @return the {@link Cookie} record
   */
  public Cookie getCookie() {
    return cookie;
  }

  /**
   * Sets the cookie configuration (called by Spring binding).
   *
   * @param cookie the cookie configuration to set
   */
  public void setCookie(Cookie cookie) {
    this.cookie = cookie;
  }

  /**
   * Returns the BCrypt configuration.
   *
   * @return the {@link Bcrypt} record
   */
  public Bcrypt getBcrypt() {
    return bcrypt;
  }

  /**
   * Sets the BCrypt configuration (called by Spring binding).
   *
   * @param bcrypt the BCrypt configuration to set
   */
  public void setBcrypt(Bcrypt bcrypt) {
    this.bcrypt = bcrypt;
  }

  /**
   * Returns the rate-limiting configuration.
   *
   * @return the {@link RateLimit} record
   */
  public RateLimit getRateLimit() {
    return rateLimit;
  }

  /**
   * Sets the rate-limiting configuration (called by Spring binding).
   *
   * @param rateLimit the rate-limit configuration to set
   */
  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  /**
   * Returns the database-specific configuration.
   *
   * @return the {@link Db} record
   */
  public Db getDb() {
    return db;
  }

  /**
   * Sets the database configuration (called by Spring binding).
   *
   * @param db the database configuration to set
   */
  public void setDb(Db db) {
    this.db = db;
  }

  /**
   * Returns the data-seeding configuration.
   *
   * @return the {@link Seed} record
   */
  public Seed getSeed() {
    return seed;
  }

  /**
   * Sets the data-seeding configuration (called by Spring binding).
   *
   * @param seed the seed configuration to set
   */
  public void setSeed(Seed seed) {
    this.seed = seed;
  }

  // ── Nested configuration records ───────────────────────────────────────────

  /**
   * CORS configuration bound from {@code app.cors.*}.
   *
   * @param allowedOrigin the single trusted frontend origin, e.g. {@code https://app.example.com}
   */
  public record Cors(@NotBlank String allowedOrigin) {}

  /**
   * HTTP cookie configuration bound from {@code app.cookie.*}.
   *
   * @param name   the cookie name used for the JWT auth cookie
   * @param secure whether the {@code Secure} flag is set (must be {@code true} in production)
   */
  public record Cookie(@NotBlank String name, boolean secure) {}

  /**
   * BCrypt password-hashing configuration bound from {@code app.bcrypt.*}.
   *
   * @param strength the work factor; must be between 10 and 14 inclusive
   */
  public record Bcrypt(@Min(10) @Max(14) int strength) {}

  /**
   * Login rate-limiting configuration bound from {@code app.rate-limit.*}.
   *
   * @param loginMaxAttempts   maximum failed login attempts before the bucket is exhausted
   * @param loginWindowMinutes the sliding-window duration in minutes
   */
  public record RateLimit(@Min(1) int loginMaxAttempts, @Min(1) int loginWindowMinutes) {}

  /**
   * Database tuning configuration bound from {@code app.db.*}.
   *
   * @param lockTimeoutMs pessimistic-lock timeout in milliseconds; must be at least 100 ms
   */
  public record Db(@Min(100) long lockTimeoutMs) {}

  /**
   * Initial data-seeding configuration bound from {@code app.seed.*}.
   *
   * @param enabled              whether the seed runner should execute on startup
   * @param adminEmail           e-mail address for the seeded admin user
   * @param adminPassword        plain-text password for the seeded admin user
   * @param staffEmail           e-mail address for the seeded staff user
   * @param staffPassword        plain-text password for the seeded staff user
   * @param demoCustomerPassword plain-text password shared by all seeded demo customers
   */
  public record Seed(
      boolean enabled,
      String adminEmail,
      String adminPassword,
      String staffEmail,
      String staffPassword,
      String demoCustomerPassword) {}
}
