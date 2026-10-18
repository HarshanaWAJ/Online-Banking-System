package com.bankingsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BillerRequest(
    @NotBlank(message = "Biller code is required")
    @Size(max = 30, message = "Code cannot exceed 30 characters")
    String code,

    @NotBlank(message = "Biller name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String name,

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category,

    @Size(max = 100, message = "Reference format cannot exceed 100 characters")
    String referenceFormat
) {}
