package com.bankingsystem.repository;

import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.OperationType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only repository for LedgerEntry.
 * Strictly guarantees that ledger records cannot be deleted or updated.
 */
public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

  LedgerEntry save(LedgerEntry entry);

  Optional<LedgerEntry> findById(Long id);

  Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

  Page<LedgerEntry> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      Long accountId, Instant from, Instant to, Pageable pageable);

  java.util.List<LedgerEntry> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(
      Long accountId, Instant from, Instant to);

  Optional<LedgerEntry> findFirstByAccountIdAndCreatedAtLessThanOrderByCreatedAtDesc(
      Long accountId, Instant timestamp);

  Optional<LedgerEntry> findByCompensatesEntryId(Long originalEntryId);

  boolean existsByCompensatesEntryId(Long originalEntryId);
}
