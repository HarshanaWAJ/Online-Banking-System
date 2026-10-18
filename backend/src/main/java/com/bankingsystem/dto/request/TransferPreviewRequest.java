package com.bankingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Preview transfer request payload for validation before final submission.
 */
public record TransferPreviewRequest(
    @NotNull(message = "Source account ID is required")
    Long sourceAccountId,

    @NotBlank(message = "Destination account number is required")
    String destinationAccountNumber,

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least LKR 0.01")
    @Digits(integer = 12, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    BigDecimal amount) {}
