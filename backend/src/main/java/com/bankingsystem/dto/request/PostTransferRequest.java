package com.bankingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Fund transfer submission payload.
 * Includes client-generated requestKey for strict duplicate submission prevention.
 */
public record PostTransferRequest(
    @NotBlank(message = "Idempotency request key is required")
    @Size(max = 64, message = "Request key cannot exceed 64 characters")
    String requestKey,

    @NotNull(message = "Source account ID is required")
    Long sourceAccountId,

    @NotBlank(message = "Destination account number is required")
    String destinationAccountNumber,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least LKR 0.01")
    @Digits(integer = 12, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    BigDecimal amount,

    @Size(max = 200, message = "Remarks cannot exceed 200 characters")
    String remarks) {}
