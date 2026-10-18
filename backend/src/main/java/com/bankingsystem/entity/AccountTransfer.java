package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/**
 * Immutable financial transfer record linking source and destination accounts.
 */
@Entity
@Immutable
@Table(
    name = "account_transfer",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_transfer_reference", columnNames = {"reference"}),
      @UniqueConstraint(
          name = "uk_transfer_initiated_request_key",
          columnNames = {"initiated_by", "request_key"})
    },
    indexes = {
      @Index(name = "idx_transfer_source", columnList = "source_account_id"),
      @Index(name = "idx_transfer_dest", columnList = "destination_account_id"),
      @Index(name = "idx_transfer_initiated_by", columnList = "initiated_by")
    })
public class AccountTransfer extends BaseEntity {

  @Column(name = "reference", nullable = false, unique = true, length = 30)
  private String reference;

  @Column(name = "request_key", nullable = false, length = 64)
  private String requestKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_account_id", nullable = false)
  private BankAccount sourceAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_account_id", nullable = false)
  private BankAccount destinationAccount;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "remarks", length = 200)
  private String remarks;

  @Column(name = "initiated_by", nullable = false)
  private Long initiatedBy;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "POSTED";

  public AccountTransfer() {}

  public AccountTransfer(
      String reference,
      String requestKey,
      BankAccount sourceAccount,
      BankAccount destinationAccount,
      BigDecimal amount,
      String remarks,
      Long initiatedBy) {
    this.reference = reference;
    this.requestKey = requestKey;
    this.sourceAccount = sourceAccount;
    this.destinationAccount = destinationAccount;
    this.amount = amount;
    this.remarks = remarks;
    this.initiatedBy = initiatedBy;
    this.status = "POSTED";
  }

  @PreUpdate
  @PreRemove
  public void preventModification() {
    throw new UnsupportedOperationException("Financial transfer records are strictly immutable.");
  }

  public String getReference() {
    return reference;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public BankAccount getSourceAccount() {
    return sourceAccount;
  }

  public BankAccount getDestinationAccount() {
    return destinationAccount;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getInitiatedBy() {
    return initiatedBy;
  }

  public String getStatus() {
    return status;
  }
}
