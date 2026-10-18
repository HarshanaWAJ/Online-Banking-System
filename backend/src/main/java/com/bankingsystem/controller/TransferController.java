package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.PostTransferRequest;
import com.bankingsystem.dto.request.TransferPreviewRequest;
import com.bankingsystem.dto.response.TransferPreviewResponse;
import com.bankingsystem.dto.response.TransferResponse;
import com.bankingsystem.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for fund transfer preview and posting.
 */
@RestController
@RequestMapping(ApiPaths.TRANSFERS)
@Tag(name = "Transfers", description = "Endpoints for funds transfer validation, preview, and atomic execution")
public class TransferController {

  private final TransferService transferService;

  public TransferController(TransferService transferService) {
    this.transferService = transferService;
  }

  @PostMapping("/preview")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Validate and preview transfer (Customer)", description = "Checks source balance and returns destination account holder's name")
  public ResponseEntity<ApiResponse<TransferPreviewResponse>> previewTransfer(
      @Valid @RequestBody TransferPreviewRequest request) {
    TransferPreviewResponse response = transferService.previewTransfer(request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Post fund transfer (Customer)", description = "Executes atomic double-entry transfer with idempotency protection")
  public ResponseEntity<ApiResponse<TransferResponse>> postTransfer(
      @Valid @RequestBody PostTransferRequest request) {
    TransferResponse response = transferService.postTransfer(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get transfer receipt by ID")
  public ResponseEntity<ApiResponse<TransferResponse>> getTransferById(@PathVariable("id") Long id) {
    TransferResponse response = transferService.getTransferById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
