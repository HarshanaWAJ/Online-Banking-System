package com.bankingsystem.repository;

import com.bankingsystem.entity.BillPayment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only repository for BillPayment transactions.
 */
public interface BillPaymentRepository extends Repository<BillPayment, Long> {

  BillPayment save(BillPayment billPayment);

  Optional<BillPayment> findById(Long id);

  Optional<BillPayment> findByReference(String reference);

  Optional<BillPayment> findBySourceAccountIdAndRequestKey(Long sourceAccountId, String requestKey);

  Page<BillPayment> findBySourceAccountIdOrderByCreatedAtDesc(Long sourceAccountId, Pageable pageable);

  Page<BillPayment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
