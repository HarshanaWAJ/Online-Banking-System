package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rejection reason payload.
 */
public record RejectRequest(
    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    String reason) {}
