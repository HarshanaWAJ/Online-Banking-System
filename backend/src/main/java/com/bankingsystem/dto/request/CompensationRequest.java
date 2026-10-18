package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Compensating reversing transaction request payload.
 */
public record CompensationRequest(
    @NotNull(message = "Original ledger entry ID is required")
    Long originalLedgerEntryId,

    @NotBlank(message = "Correction reason is mandatory")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    String reason) {}
