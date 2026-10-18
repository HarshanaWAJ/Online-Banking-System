package com.bankingsystem.repository;

import com.bankingsystem.entity.AccountTransfer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only repository for AccountTransfer. Exposes only save and find methods.
 */
public interface AccountTransferRepository extends Repository<AccountTransfer, Long> {

  AccountTransfer save(AccountTransfer transfer);

  Optional<AccountTransfer> findById(Long id);

  Optional<AccountTransfer> findByReference(String reference);

  Optional<AccountTransfer> findByInitiatedByAndRequestKey(Long initiatedBy, String requestKey);

  Page<AccountTransfer> findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
      Long sourceAccountId, Long destinationAccountId, Pageable pageable);
}
