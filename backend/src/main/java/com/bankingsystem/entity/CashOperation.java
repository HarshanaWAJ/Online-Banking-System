package com.bankingsystem.entity;

import com.bankingsystem.enums.CashOperationType;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/**
 * Immutable record of a cash deposit or cash withdrawal performed by branch staff.
 */
@Entity
@Immutable
@Table(
    name = "cash_operation",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_cash_op_performed_request_key",
          columnNames = {"performed_by", "request_key"})
    },
    indexes = {
      @Index(name = "idx_cash_op_account", columnList = "account_id"),
      @Index(name = "idx_cash_op_performed_by", columnList = "performed_by")
    })
public class CashOperation extends BaseEntity {

  @Column(name = "reference", nullable = false, unique = true, length = 30)
  private String reference;

  @Column(name = "request_key", nullable = false, length = 64)
  private String requestKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private BankAccount account;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private CashOperationType type;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "explanation", nullable = false, length = 500)
  private String explanation;

  @Column(name = "performed_by", nullable = false)
  private Long performedBy;

  @Column(name = "is_demo_funding", nullable = false)
  private boolean demoFunding = false;

  public CashOperation() {}

  public CashOperation(
      String reference,
      String requestKey,
      BankAccount account,
      CashOperationType type,
      BigDecimal amount,
      String explanation,
      Long performedBy,
      boolean demoFunding) {
    this.reference = reference;
    this.requestKey = requestKey;
    this.account = account;
    this.type = type;
    this.amount = amount;
    this.explanation = explanation;
    this.performedBy = performedBy;
    this.demoFunding = demoFunding;
  }

  @PreUpdate
  @PreRemove
  public void preventModification() {
    throw new UnsupportedOperationException("Cash operation records are immutable.");
  }

  public String getReference() {
    return reference;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public BankAccount getAccount() {
    return account;
  }

  public CashOperationType getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getExplanation() {
    return explanation;
  }

  public Long getPerformedBy() {
    return performedBy;
  }

  public boolean isDemoFunding() {
    return demoFunding;
  }
}
