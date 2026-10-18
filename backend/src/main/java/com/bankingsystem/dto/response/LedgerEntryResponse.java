package com.bankingsystem.dto.response;

import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.OperationType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger entry response representation.
 */
public record LedgerEntryResponse(
    Long id,
    Long accountId,
    String accountNumber,
    EntryDirection direction,
    BigDecimal amount,
    BigDecimal balanceAfter,
    OperationType operationType,
    String reference,
    String description,
    Long compensatesEntryId,
    Instant timestamp) {}
