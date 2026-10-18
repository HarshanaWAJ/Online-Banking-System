package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.CashOperationRequest;
import com.bankingsystem.dto.request.CompensationRequest;
import com.bankingsystem.dto.response.CashOperationResponse;
import com.bankingsystem.enums.CashOperationType;
import com.bankingsystem.service.CashOperationService;
import com.bankingsystem.service.CompensationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for teller cash deposit/withdrawal operations and compensation reversals.
 */
@RestController
@Tag(name = "Cash Operations & Compensations", description = "Teller cash desk operations and transaction reversal")
public class CashOperationController {

  private final CashOperationService cashOperationService;
  private final CompensationService compensationService;

  public CashOperationController(
      CashOperationService cashOperationService,
      CompensationService compensationService) {
    this.cashOperationService = cashOperationService;
    this.compensationService = compensationService;
  }

  @PostMapping(ApiPaths.CASH_OPERATIONS)
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Execute cash deposit or withdrawal (Staff)", description = "Posts teller cash operation with receipt reference")
  public ResponseEntity<ApiResponse<CashOperationResponse>> postCashOperation(
      @Valid @RequestBody CashOperationRequest request) {
    CashOperationResponse response = cashOperationService.postCashOperation(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping(ApiPaths.CASH_OPERATIONS)
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Search cash operations (Staff)", description = "Searches cash desk transactions by account or gets all")
  public ResponseEntity<ApiResponse<Page<CashOperationResponse>>> getCashOperations(
      @RequestParam(value = "accountId", required = false) Long accountId,
      Pageable pageable) {
    Page<CashOperationResponse> response = accountId != null
        ? cashOperationService.getCashOperationsByAccount(accountId, pageable)
        : cashOperationService.getCashOperations(pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping(ApiPaths.COMPENSATIONS)
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Post compensating reversal (Staff)", description = "Appends an immutable compensating ledger entry for an errant transaction")
  public ResponseEntity<ApiResponse<com.bankingsystem.dto.response.LedgerEntryResponse>> createCompensation(
      @Valid @RequestBody CompensationRequest request) {
    com.bankingsystem.dto.response.LedgerEntryResponse response = compensationService.createCompensation(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }
}
