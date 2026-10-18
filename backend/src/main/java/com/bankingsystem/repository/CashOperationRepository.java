package com.bankingsystem.repository;

import com.bankingsystem.entity.CashOperation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only repository for CashOperation.
 */
public interface CashOperationRepository extends Repository<CashOperation, Long> {

  CashOperation save(CashOperation cashOperation);

  Optional<CashOperation> findById(Long id);

  Optional<CashOperation> findByReference(String reference);

  Optional<CashOperation> findByPerformedByAndRequestKey(Long performedBy, String requestKey);

  Page<CashOperation> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

  Page<CashOperation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
