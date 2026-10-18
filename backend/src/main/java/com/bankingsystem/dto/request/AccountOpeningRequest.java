package com.bankingsystem.dto.request;

import com.bankingsystem.enums.AccountType;
import jakarta.validation.constraints.NotNull;

/**
 * Account opening request payload.
 */
public record AccountOpeningRequest(
    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    AccountType type) {}
