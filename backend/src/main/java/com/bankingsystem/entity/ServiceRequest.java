package com.bankingsystem.entity;

import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Customer service/support tickets handled by bank staff.
 */
@Entity
@Table(
    name = "service_request",
    indexes = {
      @Index(name = "idx_sr_customer", columnList = "customer_id"),
      @Index(name = "idx_sr_status", columnList = "status"),
      @Index(name = "idx_sr_type", columnList = "type")
    })
public class ServiceRequest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private ServiceRequestType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ServiceRequestStatus status = ServiceRequestStatus.SUBMITTED;

  @Column(name = "subject", nullable = false, length = 200)
  private String subject;

  @Column(name = "description", nullable = false, length = 2000)
  private String description;

  @Column(name = "related_account_id")
  private Long relatedAccountId;

  @Column(name = "resolution_notes", length = 2000)
  private String resolutionNotes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_staff_id")
  private AppUser assignedStaff;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  public ServiceRequest() {}

  public ServiceRequest(
      Customer customer,
      ServiceRequestType type,
      String subject,
      String description,
      Long relatedAccountId) {
    this.customer = customer;
    this.type = type;
    this.status = ServiceRequestStatus.SUBMITTED;
    this.subject = subject;
    this.description = description;
    this.relatedAccountId = relatedAccountId;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public ServiceRequestType getType() {
    return type;
  }

  public void setType(ServiceRequestType type) {
    this.type = type;
  }

  public ServiceRequestStatus getStatus() {
    return status;
  }

  public void setStatus(ServiceRequestStatus status) {
    this.status = status;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getRelatedAccountId() {
    return relatedAccountId;
  }

  public void setRelatedAccountId(Long relatedAccountId) {
    this.relatedAccountId = relatedAccountId;
  }

  public String getResolutionNotes() {
    return resolutionNotes;
  }

  public void setResolutionNotes(String resolutionNotes) {
    this.resolutionNotes = resolutionNotes;
  }

  public AppUser getAssignedStaff() {
    return assignedStaff;
  }

  public void setAssignedStaff(AppUser assignedStaff) {
    this.assignedStaff = assignedStaff;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }
}
