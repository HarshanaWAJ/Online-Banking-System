package com.bankingsystem.dto.request;

import com.bankingsystem.enums.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequestCreateDto(
    @NotNull(message = "Request type is required")
    ServiceRequestType type,

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    String subject,

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    Long relatedAccountId
) {}
