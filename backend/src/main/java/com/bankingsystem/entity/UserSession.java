package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Server-side user active session tracking.
 * Provides immediate session invalidation on logout or password change.
 */
@Entity
@Table(
    name = "user_session",
    indexes = {
      @Index(name = "idx_user_session_user_id", columnList = "user_id"),
      @Index(name = "idx_user_session_expires_at", columnList = "expires_at"),
      @Index(name = "idx_user_session_jti", columnList = "jti", unique = true)
    })
public class UserSession extends BaseEntity {

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "jti", nullable = false, unique = true, length = 100)
  private String jti;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  public UserSession() {}

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public String getJti() {
    return jti;
  }

  public void setJti(String jti) {
    this.jti = jti;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
}
