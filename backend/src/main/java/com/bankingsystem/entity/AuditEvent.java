package com.bankingsystem.entity;

import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Immutable audit trail record for privileged and critical banking operations.
 */
@Entity
@Table(
    name = "audit_event",
    indexes = {
      @Index(name = "idx_audit_event_created_at", columnList = "created_at"),
      @Index(name = "idx_audit_event_action", columnList = "action"),
      @Index(name = "idx_audit_event_actor_id", columnList = "actor_id")
    })
public class AuditEvent extends BaseEntity {

  @Column(name = "actor_id")
  private Long actorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_role", length = 20)
  private Role actorRole;

  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Column(name = "entity_type", length = 50)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome", nullable = false, length = 20)
  private AuditOutcome outcome;

  @Column(name = "details", length = 1000)
  private String details;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  public AuditEvent() {}

  public AuditEvent(
      Long actorId,
      Role actorRole,
      String action,
      String entityType,
      Long entityId,
      AuditOutcome outcome,
      String details,
      String ipAddress) {
    this.actorId = actorId;
    this.actorRole = actorRole;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.outcome = outcome;
    this.details = details;
    this.ipAddress = ipAddress;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public Role getActorRole() {
    return actorRole;
  }

  public void setActorRole(Role actorRole) {
    this.actorRole = actorRole;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(AuditOutcome outcome) {
    this.outcome = outcome;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
}
