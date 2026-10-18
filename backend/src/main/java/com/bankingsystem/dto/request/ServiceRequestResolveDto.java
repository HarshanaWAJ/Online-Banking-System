package com.bankingsystem.dto.request;

import com.bankingsystem.enums.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequestResolveDto(
    @NotNull(message = "Target status is required (RESOLVED or REJECTED)")
    ServiceRequestStatus status,

    @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
    String resolutionNotes
) {}
