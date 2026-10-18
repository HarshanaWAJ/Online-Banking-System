package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.common.PageMeta;
import com.bankingsystem.dto.request.AccountOpeningRequest;
import com.bankingsystem.dto.request.CloseAccountRequest;
import com.bankingsystem.dto.request.RejectRequest;
import com.bankingsystem.dto.response.BankAccountResponse;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing bank accounts and account lifecycle operations.
 */
@RestController
@RequestMapping(ApiPaths.ACCOUNTS)
@Tag(name = "Accounts", description = "Endpoints for bank account creation, review, lifecycle, and balance queries")
public class BankAccountController {

  private final AccountService accountService;

  public BankAccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping("/requests")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Request new bank account (Customer)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> requestAccount(
      @Valid @RequestBody AccountOpeningRequest request) {
    BankAccountResponse response = accountService.requestAccount(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
  @Operation(summary = "List accounts (Customer: own accounts; Staff/Admin: all accounts)")
  public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getAccounts(
      @RequestParam(name = "status", required = false) AccountStatus status,
      @RequestParam(name = "search", required = false) String search,
      @PageableDefault(size = 20) Pageable pageable) {

    // If staff/admin with search or pagination, return paginated directory
    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

    boolean isStaffOrAdmin = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));

    if (isStaffOrAdmin) {
      Page<BankAccountResponse> page = accountService.searchAllAccounts(status, search, pageable);
      return ResponseEntity.ok(ApiResponse.ok(page.getContent(), PageMeta.of(page)));
    } else {
      List<BankAccountResponse> myAccounts = accountService.getMyAccounts();
      return ResponseEntity.ok(ApiResponse.ok(myAccounts));
    }
  }

  @GetMapping("/requests")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Get pending account requests (Staff)")
  public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getAccountRequests(
      @RequestParam(name = "status", required = false, defaultValue = "PENDING") AccountStatus status,
      @PageableDefault(size = 15) Pageable pageable) {
    Page<BankAccountResponse> page = accountService.searchAccountRequests(status, pageable);
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), PageMeta.of(page)));
  }

  @DeleteMapping("/requests/{id}")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Cancel pending account request (Customer)")
  public ResponseEntity<ApiResponse<Void>> cancelRequest(@PathVariable("id") Long id) {
    accountService.cancelPendingRequest(id);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @PostMapping("/requests/{id}/approve")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Approve account request (Staff)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> approveRequest(@PathVariable("id") Long id) {
    BankAccountResponse response = accountService.approveAccountRequest(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/requests/{id}/reject")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Reject account request (Staff)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> rejectRequest(
      @PathVariable("id") Long id,
      @Valid @RequestBody RejectRequest request) {
    BankAccountResponse response = accountService.rejectAccountRequest(id, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN')")
  @Operation(summary = "Get account details")
  public ResponseEntity<ApiResponse<BankAccountResponse>> getAccountById(@PathVariable("id") Long id) {
    org.springframework.security.core.Authentication auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

    boolean isStaffOrAdmin = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));

    BankAccountResponse response = isStaffOrAdmin
        ? accountService.getAccountByIdForStaff(id)
        : accountService.getMyAccountById(id);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/{id}/freeze")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Freeze bank account (Staff)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> freezeAccount(@PathVariable("id") Long id) {
    BankAccountResponse response = accountService.freezeAccount(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/{id}/unfreeze")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Unfreeze bank account (Staff)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> unfreezeAccount(@PathVariable("id") Long id) {
    BankAccountResponse response = accountService.unfreezeAccount(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') and hasAuthority('APPROVE_ACCOUNTS')")
  @Operation(summary = "Close bank account (Staff)")
  public ResponseEntity<ApiResponse<BankAccountResponse>> closeAccount(
      @PathVariable("id") Long id,
      @Valid @RequestBody CloseAccountRequest request) {
    BankAccountResponse response = accountService.closeAccount(id, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
