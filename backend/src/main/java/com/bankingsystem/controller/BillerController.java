package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.BillerRequest;
import com.bankingsystem.dto.response.BillerResponse;
import com.bankingsystem.enums.BillerStatus;
import com.bankingsystem.service.BillerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BILLERS)
@Tag(name = "Billers", description = "Management and listing of utility billers")
public class BillerController {

  private final BillerService billerService;

  public BillerController(BillerService billerService) {
    this.billerService = billerService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get active billers (Customer / Staff / Admin)")
  public ResponseEntity<ApiResponse<List<BillerResponse>>> getActiveBillers() {
    List<BillerResponse> response = billerService.getActiveBillers();
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all billers paginated (Admin)")
  public ResponseEntity<ApiResponse<Page<BillerResponse>>> getAllBillers(Pageable pageable) {
    Page<BillerResponse> response = billerService.getAllBillers(pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get biller details by ID")
  public ResponseEntity<ApiResponse<BillerResponse>> getBillerById(@PathVariable("id") Long id) {
    BillerResponse response = billerService.getBillerById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create a new biller (Admin)")
  public ResponseEntity<ApiResponse<BillerResponse>> createBiller(@Valid @RequestBody BillerRequest request) {
    BillerResponse response = billerService.createBiller(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update an existing biller (Admin)")
  public ResponseEntity<ApiResponse<BillerResponse>> updateBiller(
      @PathVariable("id") Long id,
      @Valid @RequestBody BillerRequest request) {
    BillerResponse response = billerService.updateBiller(id, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Activate or retire a biller (Admin)")
  public ResponseEntity<ApiResponse<BillerResponse>> updateBillerStatus(
      @PathVariable("id") Long id,
      @RequestParam("status") BillerStatus status) {
    BillerResponse response = billerService.updateBillerStatus(id, status);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
