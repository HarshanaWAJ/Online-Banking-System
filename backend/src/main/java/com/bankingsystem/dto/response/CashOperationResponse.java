package com.bankingsystem.dto.response;

import com.bankingsystem.enums.CashOperationType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cash counter operation response.
 */
public record CashOperationResponse(
    Long id,
    String reference,
    Long accountId,
    String accountNumber,
    String customerName,
    CashOperationType type,
    BigDecimal amount,
    String explanation,
    BigDecimal balanceAfter,
    Instant timestamp) {}
