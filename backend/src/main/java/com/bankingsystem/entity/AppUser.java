package com.bankingsystem.entity;

import com.bankingsystem.enums.Role;
import com.bankingsystem.enums.StaffPermission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Authentication and authorization user entity.
 */
@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "user_permission",
      joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "permission", length = 50, nullable = false)
  private Set<StaffPermission> permissions = new HashSet<>();

  public AppUser() {}

  public AppUser(String email, String passwordHash, Role role) {
    this.email = email != null ? email.trim().toLowerCase() : null;
    this.passwordHash = passwordHash;
    this.role = role;
    this.active = true;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email != null ? email.trim().toLowerCase() : null;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public void setPasswordChangedAt(Instant passwordChangedAt) {
    this.passwordChangedAt = passwordChangedAt;
  }

  public Set<StaffPermission> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<StaffPermission> permissions) {
    this.permissions = permissions != null ? permissions : new HashSet<>();
  }

  public boolean hasPermission(StaffPermission permission) {
    return permissions.contains(permission);
  }
}
