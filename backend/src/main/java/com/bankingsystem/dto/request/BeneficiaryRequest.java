package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Beneficiary creation and update payload.
 */
public record BeneficiaryRequest(
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    String name,

    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 12, message = "Account number must be 10-12 characters")
    String accountNumber,

    @Size(max = 100, message = "Nickname cannot exceed 100 characters")
    String nickname) {}
