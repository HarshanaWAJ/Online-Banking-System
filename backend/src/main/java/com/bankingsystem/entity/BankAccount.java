package com.bankingsystem.entity;

import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Bank account entity representing accounts and account opening requests.
 */
@Entity
@Table(
    name = "bank_account",
    indexes = {
      @Index(name = "idx_bank_account_customer_id", columnList = "customer_id"),
      @Index(name = "idx_bank_account_status", columnList = "status"),
      @Index(name = "idx_bank_account_account_number", columnList = "account_number", unique = true)
    })
public class BankAccount extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "account_number", length = 12, unique = true)
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private AccountType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AccountStatus status = AccountStatus.PENDING;

  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "LKR";

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by")
  private AppUser approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejection_reason", length = 1000)
  private String rejectionReason;

  @Column(name = "closure_reason", length = 1000)
  private String closureReason;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public BankAccount() {}

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
    this.type = type;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public void setStatus(AccountStatus status) {
    this.status = status;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(Instant requestedAt) {
    this.requestedAt = requestedAt;
  }

  public AppUser getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(AppUser approvedBy) {
    this.approvedBy = approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(Instant approvedAt) {
    this.approvedAt = approvedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public String getClosureReason() {
    return closureReason;
  }

  public void setClosureReason(String closureReason) {
    this.closureReason = closureReason;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
