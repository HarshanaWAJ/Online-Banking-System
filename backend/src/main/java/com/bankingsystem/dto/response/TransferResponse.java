package com.bankingsystem.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transfer execution confirmation response.
 */
public record TransferResponse(
    Long id,
    String reference,
    String sourceAccountNumber,
    String destinationAccountNumber,
    String recipientName,
    BigDecimal amount,
    String remarks,
    String status,
    Instant timestamp) {}
