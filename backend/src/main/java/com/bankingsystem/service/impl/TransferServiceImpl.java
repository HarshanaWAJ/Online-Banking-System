package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.PostTransferRequest;
import com.bankingsystem.dto.request.TransferPreviewRequest;
import com.bankingsystem.dto.response.TransferPreviewResponse;
import com.bankingsystem.dto.response.TransferResponse;
import com.bankingsystem.entity.AccountTransfer;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.enums.OperationType;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.AccountTransferRepository;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.NotificationService;
import com.bankingsystem.service.TransferService;
import com.bankingsystem.util.MoneyUtil;
import com.bankingsystem.util.ReferenceGenerator;
import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transfer service implementing atomic double-entry bookkeeping,
 * ascending ID locking to eliminate deadlocks, and idempotency protection.
 */
@Service
public class TransferServiceImpl implements TransferService {

  private final BankAccountRepository accountRepository;
  private final AccountTransferRepository transferRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final CustomerRepository customerRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public TransferServiceImpl(
      BankAccountRepository accountRepository,
      AccountTransferRepository transferRepository,
      LedgerEntryRepository ledgerEntryRepository,
      CustomerRepository customerRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.accountRepository = accountRepository;
    this.transferRepository = transferRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.customerRepository = customerRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional(readOnly = true)
  public TransferPreviewResponse previewTransfer(TransferPreviewRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Customer profile not found"));

    BankAccount source = accountRepository.findById(request.sourceAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Source BankAccount", request.sourceAccountId()));

    if (!source.getCustomer().getId().equals(customer.getId())) {
      throw new ResourceNotFoundException("Source BankAccount", request.sourceAccountId());
    }

    if (source.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Source account is not ACTIVE. Current status: " + source.getStatus());
    }

    String destAccNum = request.destinationAccountNumber().trim();
    BankAccount dest = accountRepository.findByAccountNumber(destAccNum)
        .orElseThrow(() -> new BusinessRuleException("Destination account number '" + destAccNum + "' was not found."));

    if (source.getId().equals(dest.getId())) {
      throw new BusinessRuleException("Source and destination accounts cannot be the same.");
    }

    if (dest.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Destination account is currently not active for transactions.");
    }

    BigDecimal validAmount = MoneyUtil.requireValidAmount(request.amount());
    if (source.getBalance().compareTo(validAmount) < 0) {
      throw new BusinessRuleException(
          String.format("Insufficient funds. Available balance: LKR %s, requested: LKR %s",
              source.getBalance(), validAmount));
    }

    return new TransferPreviewResponse(
        source.getId(),
        source.getAccountNumber(),
        dest.getAccountNumber(),
        dest.getCustomer().getFullName(),
        validAmount,
        source.getBalance());
  }

  @Override
  @Transactional
  public TransferResponse postTransfer(PostTransferRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Customer profile not found"));

    String reqKey = request.requestKey().trim();

    // Idempotency check: if transfer already exists with this user & requestKey, return original record
    var existingTransfer = transferRepository.findByInitiatedByAndRequestKey(user.getId(), reqKey);
    if (existingTransfer.isPresent()) {
      return mapToResponse(existingTransfer.get());
    }

    BigDecimal transferAmount = MoneyUtil.requireValidAmount(request.amount());

    // Locate source and destination un-locked first to determine lock order
    BankAccount sourceUnchecked = accountRepository.findById(request.sourceAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Source BankAccount", request.sourceAccountId()));

    if (!sourceUnchecked.getCustomer().getId().equals(customer.getId())) {
      throw new ResourceNotFoundException("Source BankAccount", request.sourceAccountId());
    }

    String destAccNum = request.destinationAccountNumber().trim();
    BankAccount destUnchecked = accountRepository.findByAccountNumber(destAccNum)
        .orElseThrow(() -> new BusinessRuleException("Destination account number '" + destAccNum + "' was not found."));

    if (sourceUnchecked.getId().equals(destUnchecked.getId())) {
      throw new BusinessRuleException("Source and destination accounts cannot be the same.");
    }

    // PESSIMISTIC_WRITE Lock Acquisition in ASCENDING ID order to avoid database deadlocks
    Long firstLockId = Math.min(sourceUnchecked.getId(), destUnchecked.getId());
    Long secondLockId = Math.max(sourceUnchecked.getId(), destUnchecked.getId());

    BankAccount firstLocked = accountRepository.findByIdForUpdate(firstLockId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", firstLockId));
    BankAccount secondLocked = accountRepository.findByIdForUpdate(secondLockId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", secondLockId));

    BankAccount source = sourceUnchecked.getId().equals(firstLocked.getId()) ? firstLocked : secondLocked;
    BankAccount dest = destUnchecked.getId().equals(firstLocked.getId()) ? firstLocked : secondLocked;

    // Verify status on locked entities
    if (source.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Source account is not ACTIVE.");
    }
    if (dest.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Destination account is not ACTIVE.");
    }

    // Verify balance on locked entity
    if (source.getBalance().compareTo(transferAmount) < 0) {
      throw new BusinessRuleException(
          String.format("Insufficient funds. Available balance: LKR %s, requested: LKR %s",
              source.getBalance(), transferAmount));
    }

    // Debit source
    BigDecimal sourceNewBalance = source.getBalance().subtract(transferAmount);
    source.setBalance(sourceNewBalance);
    accountRepository.save(source);

    // Credit destination
    BigDecimal destNewBalance = dest.getBalance().add(transferAmount);
    dest.setBalance(destNewBalance);
    accountRepository.save(dest);

    // Create AccountTransfer
    String reference = ReferenceGenerator.generateTransferReference();
    AccountTransfer transfer = new AccountTransfer(
        reference,
        reqKey,
        source,
        dest,
        transferAmount,
        request.remarks(),
        user.getId());

    try {
      transfer = transferRepository.save(transfer);
    } catch (DataIntegrityViolationException ex) {
      // Race condition protection: re-read already committed record
      return transferRepository.findByInitiatedByAndRequestKey(user.getId(), reqKey)
          .map(this::mapToResponse)
          .orElseThrow(() -> ex);
    }

    // Double-entry bookkeeping: DEBIT entry for source account
    LedgerEntry debitEntry = new LedgerEntry(
        source,
        EntryDirection.DEBIT,
        transferAmount,
        sourceNewBalance,
        OperationType.TRANSFER,
        transfer.getId(),
        reference,
        String.format("Transfer to %s (%s)", dest.getAccountNumber(), dest.getCustomer().getFullName()),
        null,
        user.getId());
    ledgerEntryRepository.save(debitEntry);

    // Double-entry bookkeeping: CREDIT entry for destination account
    LedgerEntry creditEntry = new LedgerEntry(
        dest,
        EntryDirection.CREDIT,
        transferAmount,
        destNewBalance,
        OperationType.TRANSFER,
        transfer.getId(),
        reference,
        String.format("Transfer from %s (%s)", source.getAccountNumber(), source.getCustomer().getFullName()),
        null,
        user.getId());
    ledgerEntryRepository.save(creditEntry);

    // In-app notifications
    notificationService.createNotification(
        source.getCustomer().getUser(),
        NotificationType.TRANSFER_SENT,
        String.format("Transfer of LKR %s to %s sent. Reference: %s", transferAmount, dest.getAccountNumber(), reference),
        "/accounts/" + source.getId());

    notificationService.createNotification(
        dest.getCustomer().getUser(),
        NotificationType.TRANSFER_RECEIVED,
        String.format("Received transfer of LKR %s from %s. Reference: %s", transferAmount, source.getAccountNumber(), reference),
        "/accounts/" + dest.getId());

    // Audit event
    auditService.recordEvent(
        user.getId(),
        user.getRole(),
        "TRANSFER_POSTED",
        "AccountTransfer",
        transfer.getId(),
        AuditOutcome.SUCCESS,
        String.format("LKR %s transferred from %s to %s (Ref: %s)", transferAmount, source.getAccountNumber(), dest.getAccountNumber(), reference),
        null);

    return mapToResponse(transfer);
  }

  @Override
  @Transactional(readOnly = true)
  public TransferResponse getTransferById(Long id) {
    AppUser user = ownershipGuard.getCurrentUser();
    AccountTransfer transfer = transferRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));

    // Verify ownership
    if (!transfer.getInitiatedBy().equals(user.getId()) &&
        !user.getRole().name().equals("STAFF") &&
        !user.getRole().name().equals("ADMIN")) {
      throw new ResourceNotFoundException("Transfer", id);
    }

    return mapToResponse(transfer);
  }

  private TransferResponse mapToResponse(AccountTransfer t) {
    return new TransferResponse(
        t.getId(),
        t.getReference(),
        t.getSourceAccount().getAccountNumber(),
        t.getDestinationAccount().getAccountNumber(),
        t.getDestinationAccount().getCustomer().getFullName(),
        t.getAmount(),
        t.getRemarks(),
        t.getStatus(),
        t.getCreatedAt());
  }
}
