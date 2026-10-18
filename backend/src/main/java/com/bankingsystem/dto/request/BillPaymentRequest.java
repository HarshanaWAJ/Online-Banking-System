package com.bankingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BillPaymentRequest(
    @NotBlank(message = "Idempotency requestKey is required")
    String requestKey,

    @NotNull(message = "Source account ID is required")
    Long sourceAccountId,

    @NotNull(message = "Biller ID is required")
    Long billerId,

    @NotBlank(message = "Subscriber account / reference number is required")
    @Size(max = 100, message = "Subscriber reference cannot exceed 100 characters")
    String subscriberReference,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment is LKR 1.00")
    BigDecimal amount,

    @Size(max = 300, message = "Remarks cannot exceed 300 characters")
    String remarks
) {}
