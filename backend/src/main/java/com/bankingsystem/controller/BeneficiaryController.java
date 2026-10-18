package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.BeneficiaryRequest;
import com.bankingsystem.dto.response.BeneficiaryResponse;
import com.bankingsystem.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing customer beneficiaries.
 */
@RestController
@RequestMapping(ApiPaths.BENEFICIARIES)
@Tag(name = "Beneficiaries", description = "Endpoints for managing saved transfer recipients")
public class BeneficiaryController {

  private final BeneficiaryService beneficiaryService;

  public BeneficiaryController(BeneficiaryService beneficiaryService) {
    this.beneficiaryService = beneficiaryService;
  }

  @GetMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Get saved beneficiaries (Customer)")
  public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries() {
    List<BeneficiaryResponse> list = beneficiaryService.getMyBeneficiaries();
    return ResponseEntity.ok(ApiResponse.ok(list));
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Add new beneficiary (Customer)")
  public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
      @Valid @RequestBody BeneficiaryRequest request) {
    BeneficiaryResponse response = beneficiaryService.addBeneficiary(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Update beneficiary (Customer)")
  public ResponseEntity<ApiResponse<BeneficiaryResponse>> updateBeneficiary(
      @PathVariable("id") Long id,
      @Valid @RequestBody BeneficiaryRequest request) {
    BeneficiaryResponse response = beneficiaryService.updateBeneficiary(id, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Delete beneficiary (Customer)")
  public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(@PathVariable("id") Long id) {
    beneficiaryService.deleteBeneficiary(id);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
