package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.CashOperationRequest;
import com.bankingsystem.dto.response.CashOperationResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.CashOperation;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.CashOperationType;
import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.enums.OperationType;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.CashOperationRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.CashOperationService;
import com.bankingsystem.service.NotificationService;
import com.bankingsystem.util.MoneyUtil;
import com.bankingsystem.util.ReferenceGenerator;
import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teller Cash Desk Operation Service Implementation.
 *
 * Handles physical over-the-counter cash transactions processed by bank staff:
 * 1. Cash Deposits:
 *    - Physical currency accepted at the teller window.
 *    - Immediately credits the customer account and increments ledger balance.
 *
 * 2. Cash Withdrawals:
 *    - Physical currency disbursed to the customer at the counter.
 *    - Debits the customer account after strictly verifying available funds under a pessimistic lock.
 *
 * Concurrency & Audit Guarantees:
 * - Row-level pessimistic write lock (`SELECT ... FOR UPDATE`) guarantees accurate real-time balance calculations.
 * - Client-supplied UUID `requestKey` ensures idempotent execution on slow networks or accidental double-clicks.
 * - Every transaction produces an immutable {@link LedgerEntry} row and logs an administrative {@link com.bankingsystem.entity.AuditLog}.
 */
@Service
public class CashOperationServiceImpl implements CashOperationService {

  private final BankAccountRepository accountRepository;
  private final CashOperationRepository cashOperationRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public CashOperationServiceImpl(
      BankAccountRepository accountRepository,
      CashOperationRepository cashOperationRepository,
      LedgerEntryRepository ledgerEntryRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.accountRepository = accountRepository;
    this.cashOperationRepository = cashOperationRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  /**
   * Processes an over-the-counter cash deposit or withdrawal.
   *
   * @param request Cash operation parameters (accountId, type, amount, explanation, requestKey).
   * @return Confirmation receipt containing generated reference and new account balance.
   */
  @Override
  @Transactional
  public CashOperationResponse postCashOperation(CashOperationRequest request) {
    AppUser staff = ownershipGuard.getCurrentUser();
    String reqKey = request.requestKey().trim();

    // Step 1: Idempotency check: return existing operation if already submitted with this key
    var existing = cashOperationRepository.findByPerformedByAndRequestKey(staff.getId(), reqKey);
    if (existing.isPresent()) {
      return mapToResponse(existing.get(), existing.get().getAccount().getBalance());
    }

    BigDecimal opAmount = MoneyUtil.requireValidAmount(request.amount());

    // Step 2: Acquire row-level pessimistic write lock on target account
    BankAccount account = accountRepository.findByIdForUpdate(request.accountId())
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", request.accountId()));

    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Cash operations can only be performed on ACTIVE accounts. Current status: " + account.getStatus());
    }

    BigDecimal newBalance;
    EntryDirection direction;
    OperationType opType;

    // Step 3: Compute new balance and ledger direction based on operation type
    if (request.type() == CashOperationType.DEPOSIT) {
      newBalance = account.getBalance().add(opAmount);
      direction = EntryDirection.CREDIT;
      opType = OperationType.CASH_DEPOSIT;
    } else {
      // For withdrawals, verify that account has sufficient funds
      if (account.getBalance().compareTo(opAmount) < 0) {
        throw new BusinessRuleException(
            String.format("Insufficient funds for withdrawal. Available: LKR %s, requested: LKR %s",
                account.getBalance(), opAmount));
      }
      newBalance = account.getBalance().subtract(opAmount);
      direction = EntryDirection.DEBIT;
      opType = OperationType.CASH_WITHDRAWAL;
    }

    // Step 4: Update and persist account balance
    account.setBalance(newBalance);
    accountRepository.save(account);

    // Step 5: Create CashOperation record with unique audit reference
    String reference = ReferenceGenerator.generateCashOperationReference();
    CashOperation operation = new CashOperation(
        reference,
        reqKey,
        account,
        request.type(),
        opAmount,
        request.explanation().trim(),
        staff.getId(),
        request.type() == CashOperationType.DEPOSIT);

    try {
      operation = cashOperationRepository.save(operation);
    } catch (DataIntegrityViolationException ex) {
      // Race condition protection: re-read already committed record
      return cashOperationRepository.findByPerformedByAndRequestKey(staff.getId(), reqKey)
          .map(op -> mapToResponse(op, op.getAccount().getBalance()))
          .orElseThrow(() -> ex);
    }

    // Step 6: Append transaction record to the immutable financial ledger
    LedgerEntry entry = new LedgerEntry(
        account,
        direction,
        opAmount,
        newBalance,
        opType,
        operation.getId(),
        reference,
        String.format("Cash %s: %s", request.type(), request.explanation().trim()),
        null,
        staff.getId());
    ledgerEntryRepository.save(entry);

    // Step 7: Dispatch in-app notification to the customer
    NotificationType notifType = request.type() == CashOperationType.DEPOSIT
        ? NotificationType.CASH_DEPOSIT
        : NotificationType.CASH_WITHDRAWAL;

    notificationService.createNotification(
        account.getCustomer().getUser(),
        notifType,
        String.format("Cash %s of LKR %s processed on account %s. Ref: %s",
            request.type(), opAmount, account.getAccountNumber(), reference),
        "/accounts/" + account.getId());

    // Step 8: Record staff administrative audit log
    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "CASH_" + request.type().name(),
        "CashOperation",
        operation.getId(),
        AuditOutcome.SUCCESS,
        String.format("Cash %s of LKR %s for account %s", request.type(), opAmount, account.getAccountNumber()),
        null);

    return mapToResponse(operation, newBalance);
  }

  /**
   * Retrieves a paginated list of all teller cash operations across the bank.
   */
  @Override
  @Transactional(readOnly = true)
  public Page<CashOperationResponse> getCashOperations(Pageable pageable) {
    return cashOperationRepository.findAllByOrderByCreatedAtDesc(pageable)
        .map(op -> mapToResponse(op, op.getAccount().getBalance()));
  }

  /**
   * Retrieves a paginated list of cash operations for a specific bank account.
   */
  @Override
  @Transactional(readOnly = true)
  public Page<CashOperationResponse> getCashOperationsByAccount(Long accountId, Pageable pageable) {
    return cashOperationRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
        .map(op -> mapToResponse(op, op.getAccount().getBalance()));
  }

  /**
   * Maps a CashOperation entity to its response DTO representation.
   */
  private CashOperationResponse mapToResponse(CashOperation op, BigDecimal balanceAfter) {
    return new CashOperationResponse(
        op.getId(),
        op.getReference(),
        op.getAccount().getId(),
        op.getAccount().getAccountNumber(),
        op.getAccount().getCustomer().getFullName(),
        op.getType(),
        op.getAmount(),
        op.getExplanation(),
        balanceAfter,
        op.getCreatedAt());
  }
}
