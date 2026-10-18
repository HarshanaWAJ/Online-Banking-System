package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.common.PageMeta;
import com.bankingsystem.dto.request.UpdateProfileRequest;
import com.bankingsystem.dto.response.CustomerResponse;
import com.bankingsystem.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Customer profile management and Staff customer searches.
 */
@RestController
@RequestMapping(ApiPaths.CUSTOMERS)
@Tag(name = "Customers", description = "Endpoints for customer profiles and directory searches")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Get own customer profile")
  public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile() {
    CustomerResponse response = customerService.getMyProfile();
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/me")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Update own customer contact details")
  public ResponseEntity<ApiResponse<CustomerResponse>> updateMyProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    CustomerResponse response = customerService.updateMyProfile(request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Search customers (Staff/Admin)")
  public ResponseEntity<ApiResponse<java.util.List<CustomerResponse>>> searchCustomers(
      @RequestParam(name = "search", required = false) String search,
      @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
    Page<CustomerResponse> page = customerService.searchCustomers(search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), PageMeta.of(page)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Get customer by ID (Staff/Admin)")
  public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable("id") Long id) {
    CustomerResponse response = customerService.getCustomerById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Toggle customer account active status (Staff/Admin)")
  public ResponseEntity<ApiResponse<CustomerResponse>> toggleStatus(
      @PathVariable("id") Long id,
      @RequestParam("active") boolean active) {
    CustomerResponse response = customerService.toggleCustomerStatus(id, active);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
