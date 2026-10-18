package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.AccountOpeningRequest;
import com.bankingsystem.dto.request.CloseAccountRequest;
import com.bankingsystem.dto.request.RejectRequest;
import com.bankingsystem.dto.response.BankAccountResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.exception.AccessDeniedException;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AccountNumberService;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bank account and account lifecycle service implementation.
 */
@Service
public class AccountServiceImpl implements AccountService {

  private final BankAccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final AccountNumberService accountNumberService;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public AccountServiceImpl(
      BankAccountRepository accountRepository,
      CustomerRepository customerRepository,
      AccountNumberService accountNumberService,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.accountRepository = accountRepository;
    this.customerRepository = customerRepository;
    this.accountNumberService = accountNumberService;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional
  public BankAccountResponse requestAccount(AccountOpeningRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Only customers with registered profiles can request accounts."));

    BankAccount account = new BankAccount();
    account.setCustomer(customer);
    account.setType(request.type());
    account.setStatus(AccountStatus.PENDING);
    account.setBalance(BigDecimal.ZERO);
    account.setCurrency("LKR");
    account.setRequestedAt(Instant.now());

    account = accountRepository.save(account);

    auditService.recordEvent(
        user.getId(),
        user.getRole(),
        "ACCOUNT_REQUESTED",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Customer submitted request for " + request.type() + " account",
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BankAccountResponse> getMyAccounts() {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

    return accountRepository.findByCustomerId(customer.getId())
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public BankAccountResponse getMyAccountById(Long accountId) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (!account.getCustomer().getId().equals(customer.getId())) {
      // Leaking resource existence prevented by returning 404
      throw new ResourceNotFoundException("BankAccount", accountId);
    }

    return mapToResponse(account);
  }

  @Override
  @Transactional
  public void cancelPendingRequest(Long accountId) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (!account.getCustomer().getId().equals(customer.getId())) {
      throw new ResourceNotFoundException("BankAccount", accountId);
    }

    if (account.getStatus() != AccountStatus.PENDING) {
      throw new BusinessRuleException("Only PENDING account requests can be cancelled.");
    }

    accountRepository.delete(account);

    auditService.recordEvent(
        user.getId(),
        user.getRole(),
        "ACCOUNT_REQUEST_CANCELLED",
        "BankAccount",
        accountId,
        AuditOutcome.SUCCESS,
        "Customer cancelled pending account request",
        null);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BankAccountResponse> searchAccountRequests(AccountStatus status, Pageable pageable) {
    AccountStatus searchStatus = status != null ? status : AccountStatus.PENDING;
    return accountRepository.findByStatus(searchStatus, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional
  public BankAccountResponse approveAccountRequest(Long accountId) {
    AppUser staff = ownershipGuard.getCurrentUser();
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (account.getStatus() != AccountStatus.PENDING) {
      throw new BusinessRuleException("Only PENDING accounts can be approved. Current status: " + account.getStatus());
    }

    String accountNumber = accountNumberService.generateAccountNumber();
    account.setAccountNumber(accountNumber);
    account.setStatus(AccountStatus.ACTIVE);
    account.setApprovedBy(staff);
    account.setApprovedAt(Instant.now());

    account = accountRepository.save(account);

    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.ACCOUNT_APPROVED,
        "Your " + account.getType() + " account request was approved! Assigned Account Number: " + accountNumber,
        "/accounts/" + account.getId());

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "ACCOUNT_APPROVED",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Account approved with number " + accountNumber,
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional
  public BankAccountResponse rejectAccountRequest(Long accountId, RejectRequest request) {
    AppUser staff = ownershipGuard.getCurrentUser();
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (account.getStatus() != AccountStatus.PENDING) {
      throw new BusinessRuleException("Only PENDING accounts can be rejected.");
    }

    account.setStatus(AccountStatus.CLOSED);
    account.setClosureReason("REQUEST_REJECTED: " + request.reason().trim());
    account.setRejectionReason(request.reason().trim());
    account.setClosedAt(Instant.now());

    account = accountRepository.save(account);

    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.ACCOUNT_REJECTED,
        "Your account request was not approved. Reason: " + request.reason(),
        "/dashboard");

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "ACCOUNT_REJECTED",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Account request rejected. Reason: " + request.reason(),
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional
  public BankAccountResponse freezeAccount(Long accountId) {
    AppUser staff = ownershipGuard.getCurrentUser();
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (!account.getStatus().canTransitionTo(AccountStatus.FROZEN)) {
      throw new BusinessRuleException("Cannot freeze account in current status: " + account.getStatus());
    }

    account.setStatus(AccountStatus.FROZEN);
    account = accountRepository.save(account);

    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.ACCOUNT_FROZEN,
        "Your account " + account.getAccountNumber() + " has been temporarily frozen. Please contact support.",
        "/accounts/" + account.getId());

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "ACCOUNT_FROZEN",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Account " + account.getAccountNumber() + " frozen by staff",
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional
  public BankAccountResponse unfreezeAccount(Long accountId) {
    AppUser staff = ownershipGuard.getCurrentUser();
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (account.getStatus() != AccountStatus.FROZEN) {
      throw new BusinessRuleException("Only FROZEN accounts can be unfrozen.");
    }

    account.setStatus(AccountStatus.ACTIVE);
    account = accountRepository.save(account);

    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.ACCOUNT_UNFROZEN,
        "Your account " + account.getAccountNumber() + " has been reactivated.",
        "/accounts/" + account.getId());

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "ACCOUNT_UNFROZEN",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Account " + account.getAccountNumber() + " unfrozen by staff",
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional
  public BankAccountResponse closeAccount(Long accountId, CloseAccountRequest request) {
    AppUser staff = ownershipGuard.getCurrentUser();
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (!account.getStatus().canTransitionTo(AccountStatus.CLOSED)) {
      throw new BusinessRuleException("Account is already closed or in an invalid state for closure.");
    }

    if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessRuleException(
          "Account balance must be exactly LKR 0.00 before closing. Current balance: LKR " + account.getBalance());
    }

    account.setStatus(AccountStatus.CLOSED);
    account.setClosureReason(request.reason().trim());
    account.setClosedAt(Instant.now());
    account = accountRepository.save(account);

    notificationService.createNotification(
        account.getCustomer().getUser(),
        NotificationType.ACCOUNT_CLOSED,
        "Your account " + account.getAccountNumber() + " has been permanently closed.",
        "/dashboard");

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "ACCOUNT_CLOSED",
        "BankAccount",
        account.getId(),
        AuditOutcome.SUCCESS,
        "Account " + account.getAccountNumber() + " closed. Reason: " + request.reason(),
        null);

    return mapToResponse(account);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BankAccountResponse> searchAllAccounts(AccountStatus status, String search, Pageable pageable) {
    return accountRepository.searchAccounts(status, search, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BankAccountResponse getAccountByIdForStaff(Long accountId) {
    BankAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));
    return mapToResponse(account);
  }

  private BankAccountResponse mapToResponse(BankAccount a) {
    String email = null;
    if (a.getCustomer() != null && a.getCustomer().getUser() != null) {
      email = a.getCustomer().getUser().getEmail();
    }
    String nic = a.getCustomer() != null ? a.getCustomer().getNic() : null;

    return new BankAccountResponse(
        a.getId(),
        a.getCustomer().getId(),
        a.getCustomer().getFullName(),
        nic,
        email,
        a.getAccountNumber(),
        a.getType(),
        a.getStatus(),
        a.getBalance(),
        a.getCurrency(),
        a.getRequestedAt(),
        a.getApprovedAt(),
        a.getRejectionReason(),
        a.getClosureReason(),
        a.getClosedAt());
  }
}
