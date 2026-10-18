package com.bankingsystem.service.impl;

import com.bankingsystem.entity.AccountNumberSequence;
import com.bankingsystem.repository.AccountNumberSequenceRepository;
import com.bankingsystem.service.AccountNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account number generator utilizing row-level pessimistic locking on sequence table.
 */
@Service
public class AccountNumberServiceImpl implements AccountNumberService {

  private final AccountNumberSequenceRepository sequenceRepository;

  public AccountNumberServiceImpl(AccountNumberSequenceRepository sequenceRepository) {
    this.sequenceRepository = sequenceRepository;
  }

  @Override
  @Transactional
  public String generateAccountNumber() {
    AccountNumberSequence sequence = sequenceRepository.findByIdForUpdate(1L)
        .orElseGet(() -> sequenceRepository.save(new AccountNumberSequence(1L, 100000000000L)));

    long assigned = sequence.getNextValue();
    sequence.setNextValue(assigned + 1);
    sequenceRepository.save(sequence);

    return String.valueOf(assigned);
  }
}
