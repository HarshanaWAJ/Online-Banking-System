package com.bankingsystem.dto.response;

import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AccountType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Bank account and account request response.
 */
public record BankAccountResponse(
    Long id,
    Long customerId,
    String customerName,
    String customerNic,
    String customerEmail,
    String accountNumber,
    AccountType type,
    AccountStatus status,
    BigDecimal balance,
    String currency,
    Instant requestedAt,
    Instant approvedAt,
    String rejectionReason,
    String closureReason,
    Instant closedAt) {

  public BankAccountResponse(
      Long id,
      Long customerId,
      String customerName,
      String accountNumber,
      AccountType type,
      AccountStatus status,
      BigDecimal balance,
      String currency,
      Instant requestedAt,
      Instant approvedAt,
      String rejectionReason,
      String closureReason,
      Instant closedAt) {
    this(
        id,
        customerId,
        customerName,
        null,
        null,
        accountNumber,
        type,
        status,
        balance,
        currency,
        requestedAt,
        approvedAt,
        rejectionReason,
        closureReason,
        closedAt);
  }
}
