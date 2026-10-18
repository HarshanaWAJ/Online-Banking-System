package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.BillPaymentRequest;
import com.bankingsystem.dto.response.BillPaymentResponse;
import com.bankingsystem.service.BillPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BILL_PAYMENTS)
@Tag(name = "Bill Payments", description = "Utility bill payment processing and history")
public class BillPaymentController {

  private final BillPaymentService billPaymentService;

  public BillPaymentController(BillPaymentService billPaymentService) {
    this.billPaymentService = billPaymentService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Pay a utility bill (Customer)", description = "Debits customer account and creates bill payment record")
  public ResponseEntity<ApiResponse<BillPaymentResponse>> payBill(
      @Valid @RequestBody BillPaymentRequest request) {
    BillPaymentResponse response = billPaymentService.payBill(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get bill payment history (Customer / Staff / Admin)")
  public ResponseEntity<ApiResponse<Page<BillPaymentResponse>>> getMyBillPayments(
      @RequestParam(value = "accountId", required = false) Long accountId,
      Pageable pageable) {
    Page<BillPaymentResponse> response = billPaymentService.getMyBillPayments(accountId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get bill payment receipt by ID")
  public ResponseEntity<ApiResponse<BillPaymentResponse>> getBillPaymentById(@PathVariable("id") Long id) {
    BillPaymentResponse response = billPaymentService.getBillPaymentById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
