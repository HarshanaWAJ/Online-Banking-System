package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.CompensationRequest;
import com.bankingsystem.dto.response.LedgerEntryResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.enums.OperationType;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.CompensationService;
import com.bankingsystem.service.NotificationService;
import com.bankingsystem.util.ReferenceGenerator;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensation service implementation.
 * Guarantees that posted financial records remain immutable,
 * creating linked compensating reversing entries instead of edits.
 */
@Service
public class CompensationServiceImpl implements CompensationService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final BankAccountRepository accountRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public CompensationServiceImpl(
      LedgerEntryRepository ledgerEntryRepository,
      BankAccountRepository accountRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.accountRepository = accountRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional
  public LedgerEntryResponse createCompensation(CompensationRequest request) {
    AppUser staff = ownershipGuard.getCurrentUser();

    LedgerEntry original = ledgerEntryRepository.findById(request.originalLedgerEntryId())
        .orElseThrow(() -> new ResourceNotFoundException("LedgerEntry", request.originalLedgerEntryId()));

    // Rule: an entry can be compensated only once
    if (ledgerEntryRepository.existsByCompensatesEntryId(original.getId())) {
      throw new BusinessRuleException("This ledger entry has already been compensated.");
    }

    if (original.getOperationType() == OperationType.COMPENSATION) {
      throw new BusinessRuleException("Cannot compensate an entry that is itself a compensating entry.");
    }

    // Lock account
    BankAccount account = accountRepository.findByIdForUpdate(original.getAccount().getId())
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", original.getAccount().getId()));

    if (account.getStatus() == AccountStatus.CLOSED) {
      throw new BusinessRuleException("Cannot post compensation on a CLOSED account.");
    }

    // Determine reversing direction
    EntryDirection reversingDirection = original.getDirection() == EntryDirection.DEBIT
        ? EntryDirection.CREDIT
        : EntryDirection.DEBIT;

    BigDecimal newBalance;
    if (reversingDirection == EntryDirection.CREDIT) {
      newBalance = account.getBalance().add(original.getAmount());
    } else {
      // Reversal debits the account — verify balance
      if (account.getBalance().compareTo(original.getAmount()) < 0) {
        throw new BusinessRuleException(
            String.format("Insufficient funds to compensate entry. Available: LKR %s, required: LKR %s",
                account.getBalance(), original.getAmount()));
      }
      newBalance = account.getBalance().subtract(original.getAmount());
    }

    account.setBalance(newBalance);
    accountRepository.save(account);

    String reference = ReferenceGenerator.generateCompensationReference();
    String description = String.format("Compensation for [%s]: %s", original.getReference(), request.reason().trim());

    LedgerEntry compensationEntry = new LedgerEntry(
        account,
        reversingDirection,
        original.getAmount(),
        newBalance,
        OperationType.COMPENSATION,
        original.getId(),
        reference,
        description,
        original.getId(),
        staff.getId());

    compensationEntry = ledgerEntryRepository.save(compensationEntry);

    // Notify customer
    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.SECURITY_ALERT,
        String.format("A correction entry of LKR %s was applied to your account %s (Ref: %s)",
            original.getAmount(), account.getAccountNumber(), reference),
        "/accounts/" + account.getId());

    // Audit log
    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "COMPENSATION_CREATED",
        "LedgerEntry",
        compensationEntry.getId(),
        AuditOutcome.SUCCESS,
        String.format("Compensated original entry #%d with reference %s. Reason: %s",
            original.getId(), reference, request.reason().trim()),
        null);

    return new LedgerEntryResponse(
        compensationEntry.getId(),
        account.getId(),
        account.getAccountNumber(),
        compensationEntry.getDirection(),
        compensationEntry.getAmount(),
        compensationEntry.getBalanceAfter(),
        compensationEntry.getOperationType(),
        compensationEntry.getReference(),
        compensationEntry.getDescription(),
        compensationEntry.getCompensatesEntryId(),
        compensationEntry.getCreatedAt());
  }
}
