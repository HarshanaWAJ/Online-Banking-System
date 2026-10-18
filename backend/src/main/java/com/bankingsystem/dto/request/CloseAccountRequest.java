package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Account closure reason payload.
 */
public record CloseAccountRequest(
    @NotBlank(message = "Closure reason is required")
    @Size(max = 1000, message = "Closure reason cannot exceed 1000 characters")
    String reason) {}
