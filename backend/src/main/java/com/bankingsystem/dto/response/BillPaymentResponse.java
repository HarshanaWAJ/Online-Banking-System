package com.bankingsystem.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentResponse(
    Long id,
    String reference,
    Long sourceAccountId,
    String sourceAccountNumber,
    Long billerId,
    String billerName,
    String billerCategory,
    String subscriberReference,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String remarks,
    Instant timestamp
) {}
