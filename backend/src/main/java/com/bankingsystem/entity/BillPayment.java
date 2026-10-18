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
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/**
 * Immutable bill payment transaction record.
 * Financial records cannot be modified or deleted.
 */
@Entity
@Immutable
@Table(
    name = "bill_payment",
    indexes = {
      @Index(name = "idx_bill_payment_ref", columnList = "reference", unique = true),
      @Index(name = "idx_bill_payment_source", columnList = "source_account_id"),
      @Index(name = "idx_bill_payment_biller", columnList = "biller_id"),
      @Index(name = "idx_bill_payment_reqkey", columnList = "source_account_id, request_key", unique = true)
    })
public class BillPayment extends BaseEntity {

  @Column(name = "reference", nullable = false, unique = true, length = 50)
  private String reference;

  @Column(name = "request_key", nullable = false, length = 100)
  private String requestKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_account_id", nullable = false)
  private BankAccount sourceAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "biller_id", nullable = false)
  private Biller biller;

  @Column(name = "subscriber_reference", nullable = false, length = 100)
  private String subscriberReference;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "remarks", length = 300)
  private String remarks;

  public BillPayment() {}

  public BillPayment(
      String reference,
      String requestKey,
      BankAccount sourceAccount,
      Biller biller,
      String subscriberReference,
      BigDecimal amount,
      String remarks) {
    this.reference = reference;
    this.requestKey = requestKey;
    this.sourceAccount = sourceAccount;
    this.biller = biller;
    this.subscriberReference = subscriberReference;
    this.amount = amount;
    this.remarks = remarks;
  }

  @PreUpdate
  @PreRemove
  public void preventModification() {
    throw new UnsupportedOperationException("Bill payment records are strictly immutable and cannot be updated or deleted.");
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

  public Biller getBiller() {
    return biller;
  }

  public String getSubscriberReference() {
    return subscriberReference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRemarks() {
    return remarks;
  }
}
