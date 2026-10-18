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
 * Enterprise Transfer Service Implementation.
 *
 * Core Engineering & Financial Guarantees:
 * 1. Concurrency Control & Deadlock Elimination:
 *    - To transfer funds between two accounts (A and B), locks must be acquired on both account rows.
 *    - If User 1 transfers from A to B while User 2 transfers from B to A simultaneously, a circular wait deadlock occurs.
 *    - Algorithm: We acquire pessimistic write locks (`SELECT ... FOR UPDATE`) strictly in ASCENDING account ID order:
 *      {@code firstLockId = min(A.id, B.id); secondLockId = max(A.id, B.id);}
 *      Because all concurrent threads acquire row locks in the exact same sequence, circular dependency is mathematically impossible.
 *
 * 2. Strict Double-Entry Bookkeeping:
 *    - Money is never created or destroyed: every transfer produces exactly two synchronized {@link LedgerEntry} rows:
 *      a) A DEBIT entry reducing the source account balance.
 *      b) A CREDIT entry increasing the destination account balance.
 *    - Both entries record the identical transaction reference and snapshot the account balance post-mutation.
 *
 * 3. Idempotency Protection:
 *    - Every transfer submission includes a client-generated UUID `requestKey`.
 *    - If network lag causes duplicate submits or retries, the server detects the existing (userId, requestKey)
 *      tuple and safely returns the original transfer receipt without executing a second balance deduction.
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

  /**
   * Preview a transfer prior to execution (Dry-run validation).
   *
   * Verifies account ownership, account status, destination account validity, and balance sufficiency
   * without locking rows or modifying the database ledger.
   *
   * @param request Transfer preview payload.
   * @return Preview response containing verified recipient name and available balance.
   */
  @Override
  @Transactional(readOnly = true)
  public TransferPreviewResponse previewTransfer(TransferPreviewRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Customer profile not found"));

    // Verify source account exists and belongs to the authenticated customer
    BankAccount source = accountRepository.findById(request.sourceAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Source BankAccount", request.sourceAccountId()));

    if (!source.getCustomer().getId().equals(customer.getId())) {
      throw new ResourceNotFoundException("Source BankAccount", request.sourceAccountId());
    }

    if (source.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Source account is not ACTIVE. Current status: " + source.getStatus());
    }

    // Verify destination account exists and is active
    String destAccNum = request.destinationAccountNumber().trim();
    BankAccount dest = accountRepository.findByAccountNumber(destAccNum)
        .orElseThrow(() -> new BusinessRuleException("Destination account number '" + destAccNum + "' was not found."));

    // Prevent transferring funds to the exact same account
    if (source.getId().equals(dest.getId())) {
      throw new BusinessRuleException("Source and destination accounts cannot be the same.");
    }

    if (dest.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Destination account is currently not active for transactions.");
    }

    // Verify amount positivity and scale
    BigDecimal validAmount = MoneyUtil.requireValidAmount(request.amount());

    // Verify preliminary balance sufficiency
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

  /**
   * Executes an atomic inter-account fund transfer.
   *
   * Workflow:
   * 1. Idempotency Check: Verifies if (userId, requestKey) was previously processed.
   * 2. Lock Acquisition: Acquires pessimistic write locks on both accounts in ascending ID order.
   * 3. Balance Re-Verification: Ensures funds are still sufficient under the row lock.
   * 4. Double-Entry Mutation: Updates account balances, inserts debit/credit ledger records.
   * 5. Notifications & Audit: Dispatches in-app notifications to both parties and logs audit event.
   *
   * @param request Confirmed transfer request payload with requestKey.
   * @return Immutable transfer confirmation response.
   */
  @Override
  @Transactional
  public TransferResponse postTransfer(PostTransferRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Customer profile not found"));

    String reqKey = request.requestKey().trim();

    // Step 1: Idempotency check: if transfer already exists with this user & requestKey, return original record
    var existingTransfer = transferRepository.findByInitiatedByAndRequestKey(user.getId(), reqKey);
    if (existingTransfer.isPresent()) {
      return mapToResponse(existingTransfer.get());
    }

    BigDecimal transferAmount = MoneyUtil.requireValidAmount(request.amount());

    // Step 2: Locate source and destination un-locked first to determine lock acquisition order
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

    // Step 3: PESSIMISTIC_WRITE Lock Acquisition in ASCENDING ID order to eliminate database deadlocks
    Long firstLockId = Math.min(sourceUnchecked.getId(), destUnchecked.getId());
    Long secondLockId = Math.max(sourceUnchecked.getId(), destUnchecked.getId());

    BankAccount firstLocked = accountRepository.findByIdForUpdate(firstLockId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", firstLockId));
    BankAccount secondLocked = accountRepository.findByIdForUpdate(secondLockId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", secondLockId));

    BankAccount source = sourceUnchecked.getId().equals(firstLocked.getId()) ? firstLocked : secondLocked;
    BankAccount dest = destUnchecked.getId().equals(firstLocked.getId()) ? firstLocked : secondLocked;

    // Step 4: Verify status on locked entities
    if (source.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Source account is not ACTIVE.");
    }
    if (dest.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Destination account is not ACTIVE.");
    }

    // Step 5: Verify balance on locked entity (authoritative check)
    if (source.getBalance().compareTo(transferAmount) < 0) {
      throw new BusinessRuleException(
          String.format("Insufficient funds. Available balance: LKR %s, requested: LKR %s",
              source.getBalance(), transferAmount));
    }

    // Step 6: Debit source account
    BigDecimal sourceNewBalance = source.getBalance().subtract(transferAmount);
    source.setBalance(sourceNewBalance);
    accountRepository.save(source);

    // Step 7: Credit destination account
    BigDecimal destNewBalance = dest.getBalance().add(transferAmount);
    dest.setBalance(destNewBalance);
    accountRepository.save(dest);

    // Step 8: Create and persist AccountTransfer audit record
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
      // Race condition protection: re-read already committed record if duplicate request key arrived concurrently
      return transferRepository.findByInitiatedByAndRequestKey(user.getId(), reqKey)
          .map(this::mapToResponse)
          .orElseThrow(() -> ex);
    }

    // Step 9: Double-entry bookkeeping: DEBIT entry for source account
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

    // Step 10: Double-entry bookkeeping: CREDIT entry for destination account
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

    // Step 11: Dispatch in-app notifications to sender and receiver
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

    // Step 12: Record security audit log
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

  /**
   * Retrieves a transfer record by ID with ownership enforcement.
   */
  @Override
  @Transactional(readOnly = true)
  public TransferResponse getTransferById(Long id) {
    AppUser user = ownershipGuard.getCurrentUser();
    AccountTransfer transfer = transferRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transfer", id));

    // Non-staff/admin users can only view transfers they initiated
    if (!transfer.getInitiatedBy().equals(user.getId()) &&
        !user.getRole().name().equals("STAFF") &&
        !user.getRole().name().equals("ADMIN")) {
      throw new ResourceNotFoundException("Transfer", id);
    }

    return mapToResponse(transfer);
  }

  /**
   * Maps an internal AccountTransfer entity to a public TransferResponse DTO.
   */
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
