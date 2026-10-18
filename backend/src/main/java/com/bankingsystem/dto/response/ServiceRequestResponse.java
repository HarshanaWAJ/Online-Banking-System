package com.bankingsystem.dto.response;

import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import java.time.Instant;

public record ServiceRequestResponse(
    Long id,
    Long customerId,
    String customerName,
    String customerEmail,
    ServiceRequestType type,
    ServiceRequestStatus status,
    String subject,
    String description,
    Long relatedAccountId,
    String resolutionNotes,
    Long assignedStaffId,
    String assignedStaffEmail,
    Instant resolvedAt,
    Instant createdAt
) {}
