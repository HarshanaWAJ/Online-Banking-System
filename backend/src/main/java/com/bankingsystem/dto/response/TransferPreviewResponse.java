package com.bankingsystem.dto.response;

import java.math.BigDecimal;

/**
 * Transfer validation preview response displaying recipient info for customer confirmation.
 */
public record TransferPreviewResponse(
    Long sourceAccountId,
    String sourceAccountNumber,
    String destinationAccountNumber,
    String recipientName,
    BigDecimal amount,
    BigDecimal availableBalance) {}
