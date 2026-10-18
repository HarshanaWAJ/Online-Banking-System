package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.BillPaymentRequest;
import com.bankingsystem.dto.response.BillPaymentResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.BillPayment;
import com.bankingsystem.entity.Biller;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.BillerStatus;
import com.bankingsystem.enums.EntryDirection;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.enums.OperationType;
import com.bankingsystem.enums.Role;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.BillPaymentRepository;
import com.bankingsystem.repository.BillerRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.BillPaymentService;
import com.bankingsystem.service.NotificationService;
import com.bankingsystem.util.MoneyUtil;
import com.bankingsystem.util.ReferenceGenerator;
import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {

  private final BillPaymentRepository billPaymentRepository;
  private final BankAccountRepository accountRepository;
  private final BillerRepository billerRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public BillPaymentServiceImpl(
      BillPaymentRepository billPaymentRepository,
      BankAccountRepository accountRepository,
      BillerRepository billerRepository,
      LedgerEntryRepository ledgerEntryRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.billPaymentRepository = billPaymentRepository;
    this.accountRepository = accountRepository;
    this.billerRepository = billerRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional
  public BillPaymentResponse payBill(BillPaymentRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    String reqKey = request.requestKey().trim();

    // Idempotency check: if already processed for this source account + requestKey, return existing receipt
    var existing = billPaymentRepository.findBySourceAccountIdAndRequestKey(request.sourceAccountId(), reqKey);
    if (existing.isPresent()) {
      return mapToResponse(existing.get(), existing.get().getSourceAccount().getBalance());
    }

    BigDecimal paymentAmount = MoneyUtil.requireValidAmount(request.amount());

    // Acquire pessimistic write lock
    BankAccount sourceAccount = accountRepository.findByIdForUpdate(request.sourceAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", request.sourceAccountId()));

    // Verify ownership
    if (!sourceAccount.getCustomer().getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("BankAccount", request.sourceAccountId());
    }

    if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
      throw new BusinessRuleException("Source account is not ACTIVE. Current status: " + sourceAccount.getStatus());
    }

    Biller biller = billerRepository.findById(request.billerId())
        .orElseThrow(() -> new ResourceNotFoundException("Biller", request.billerId()));

    if (biller.getStatus() != BillerStatus.ACTIVE) {
      throw new BusinessRuleException("The selected biller is currently inactive.");
    }

    // Verify balance
    if (sourceAccount.getBalance().compareTo(paymentAmount) < 0) {
      throw new BusinessRuleException(
          String.format("Insufficient funds. Available: LKR %s, payment amount: LKR %s",
              sourceAccount.getBalance(), paymentAmount));
    }

    // Debit source account
    BigDecimal newBalance = sourceAccount.getBalance().subtract(paymentAmount);
    sourceAccount.setBalance(newBalance);
    accountRepository.save(sourceAccount);

    String reference = ReferenceGenerator.generateBillPaymentReference();
    BillPayment payment = new BillPayment(
        reference,
        reqKey,
        sourceAccount,
        biller,
        request.subscriberReference().trim(),
        paymentAmount,
        request.remarks() != null ? request.remarks().trim() : null
    );

    try {
      payment = billPaymentRepository.save(payment);
    } catch (DataIntegrityViolationException ex) {
      return billPaymentRepository.findBySourceAccountIdAndRequestKey(sourceAccount.getId(), reqKey)
          .map(bp -> mapToResponse(bp, bp.getSourceAccount().getBalance()))
          .orElseThrow(() -> ex);
    }

    // Append double-entry ledger record
    LedgerEntry ledgerEntry = new LedgerEntry(
        sourceAccount,
        EntryDirection.DEBIT,
        paymentAmount,
        newBalance,
        OperationType.BILL_PAYMENT,
        payment.getId(),
        reference,
        String.format("Bill Payment to %s (Ref: %s)", biller.getName(), request.subscriberReference().trim()),
        null,
        user.getId()
    );
    ledgerEntryRepository.save(ledgerEntry);

    // Notify customer
    notificationService.createNotification(
        user,
        NotificationType.BILL_PAID,
        String.format("Bill payment of LKR %s to %s was successful. Ref: %s",
            paymentAmount, biller.getName(), reference),
        "/accounts/" + sourceAccount.getId() + "/transactions"
    );

    // Audit log
    auditService.recordEvent(
        user.getId(),
        user.getRole(),
        "BILL_PAYMENT",
        "BillPayment",
        payment.getId(),
        AuditOutcome.SUCCESS,
        String.format("Paid LKR %s to %s from account %s", paymentAmount, biller.getName(), sourceAccount.getAccountNumber()),
        null
    );

    return mapToResponse(payment, newBalance);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BillPaymentResponse> getMyBillPayments(Long accountId, Pageable pageable) {
    AppUser user = ownershipGuard.getCurrentUser();
    if (accountId != null) {
      BankAccount account = accountRepository.findById(accountId)
          .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));
      if (user.getRole() == Role.CUSTOMER && !account.getCustomer().getUser().getId().equals(user.getId())) {
        throw new ResourceNotFoundException("BankAccount", accountId);
      }
      return billPaymentRepository.findBySourceAccountIdOrderByCreatedAtDesc(accountId, pageable)
          .map(bp -> mapToResponse(bp, bp.getSourceAccount().getBalance()));
    }

    return billPaymentRepository.findAllByOrderByCreatedAtDesc(pageable)
        .map(bp -> mapToResponse(bp, bp.getSourceAccount().getBalance()));
  }

  @Override
  @Transactional(readOnly = true)
  public BillPaymentResponse getBillPaymentById(Long id) {
    AppUser user = ownershipGuard.getCurrentUser();
    BillPayment payment = billPaymentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("BillPayment", id));

    if (user.getRole() == Role.CUSTOMER &&
        !payment.getSourceAccount().getCustomer().getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("BillPayment", id);
    }

    return mapToResponse(payment, payment.getSourceAccount().getBalance());
  }

  private BillPaymentResponse mapToResponse(BillPayment bp, BigDecimal balanceAfter) {
    return new BillPaymentResponse(
        bp.getId(),
        bp.getReference(),
        bp.getSourceAccount().getId(),
        bp.getSourceAccount().getAccountNumber(),
        bp.getBiller().getId(),
        bp.getBiller().getName(),
        bp.getBiller().getCategory(),
        bp.getSubscriberReference(),
        bp.getAmount(),
        balanceAfter,
        bp.getRemarks(),
        bp.getCreatedAt()
    );
  }
}
