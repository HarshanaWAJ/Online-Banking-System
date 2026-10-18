package com.bankingsystem.service;

import com.bankingsystem.dto.response.LedgerEntryResponse;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service managing ledger entry queries and statement generation.
 */
public interface StatementService {

  Page<LedgerEntryResponse> getAccountTransactions(
      Long accountId,
      Instant from,
      Instant to,
      Pageable pageable);

  byte[] exportStatementCsv(Long accountId, Instant from, Instant to);

  byte[] exportStatementPdf(Long accountId, Instant from, Instant to);
}
