package com.bankingsystem.dto.request;

import com.bankingsystem.enums.CashOperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Staff cash deposit / withdrawal payload.
 */
public record CashOperationRequest(
    @NotBlank(message = "Idempotency request key is required")
    String requestKey,

    @NotNull(message = "Account ID is required")
    Long accountId,

    @NotNull(message = "Operation type is required (DEPOSIT or WITHDRAWAL)")
    CashOperationType type,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least LKR 0.01")
    @Digits(integer = 12, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    BigDecimal amount,

    @NotBlank(message = "Explanation / counter note is required")
    @Size(max = 500, message = "Explanation cannot exceed 500 characters")
    String explanation) {}
