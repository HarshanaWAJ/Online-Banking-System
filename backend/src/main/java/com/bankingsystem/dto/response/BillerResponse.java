package com.bankingsystem.dto.response;

import com.bankingsystem.enums.BillerStatus;
import java.time.Instant;

public record BillerResponse(
    Long id,
    String code,
    String name,
    String category,
    String referenceFormat,
    BillerStatus status,
    Instant createdAt
) {}
