package com.bankingsystem.service;

import com.bankingsystem.dto.request.CompensationRequest;
import com.bankingsystem.dto.response.LedgerEntryResponse;

/**
 * Service managing compensating reversal ledger entries.
 */
public interface CompensationService {

  LedgerEntryResponse createCompensation(CompensationRequest request);
}
