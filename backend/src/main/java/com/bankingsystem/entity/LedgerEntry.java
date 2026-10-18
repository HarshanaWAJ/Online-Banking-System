package com.bankingsystem.entity;

import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/**
 * Immutable financial double-entry ledger record.
 * Every debit and credit in the system is recorded here with the resulting balance.
 */
@Entity
@Immutable
@Table(
    name = "ledger_entry",
    indexes = {
      @Index(name = "idx_ledger_account_created", columnList = "account_id, created_at"),
      @Index(name = "idx_ledger_reference", columnList = "reference"),
      @Index(name = "idx_ledger_operation", columnList = "operation_type, operation_id")
    })
public class LedgerEntry extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private BankAccount account;

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 10)
  private EntryDirection direction;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 30)
  private OperationType operationType;

  @Column(name = "operation_id")
  private Long operationId;

  @Column(name = "reference", nullable = false, length = 50)
  private String reference;

  @Column(name = "description", nullable = false, length = 300)
  private String description;

  @Column(name = "compensates_entry_id", unique = true)
  private Long compensatesEntryId;

  @Column(name = "created_by")
  private Long createdBy;

  public LedgerEntry() {}

  public LedgerEntry(
      BankAccount account,
      EntryDirection direction,
      BigDecimal amount,
      BigDecimal balanceAfter,
      OperationType operationType,
      Long operationId,
      String reference,
      String description,
      Long compensatesEntryId,
      Long createdBy) {
    this.account = account;
    this.direction = direction;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.operationType = operationType;
    this.operationId = operationId;
    this.reference = reference;
    this.description = description;
    this.compensatesEntryId = compensatesEntryId;
    this.createdBy = createdBy;
  }

  @PreUpdate
  @PreRemove
  public void preventModification() {
    throw new UnsupportedOperationException("Ledger records are strictly immutable and cannot be updated or deleted.");
  }

  public BankAccount getAccount() {
    return account;
  }

  public EntryDirection getDirection() {
    return direction;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBalanceAfter() {
    return balanceAfter;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public Long getOperationId() {
    return operationId;
  }

  public String getReference() {
    return reference;
  }

  public String getDescription() {
    return description;
  }

  public Long getCompensatesEntryId() {
    return compensatesEntryId;
  }

  public Long getCreatedBy() {
    return createdBy;
  }
}
