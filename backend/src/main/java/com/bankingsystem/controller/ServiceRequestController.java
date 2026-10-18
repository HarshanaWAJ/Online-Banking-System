package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.ServiceRequestCreateDto;
import com.bankingsystem.dto.request.ServiceRequestResolveDto;
import com.bankingsystem.dto.response.ServiceRequestResponse;
import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import com.bankingsystem.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SERVICE_REQUESTS)
@Tag(name = "Service Requests", description = "Customer service tickets and support queue")
public class ServiceRequestController {

  private final ServiceRequestService serviceRequestService;

  public ServiceRequestController(ServiceRequestService serviceRequestService) {
    this.serviceRequestService = serviceRequestService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Submit a service/support ticket (Customer)")
  public ResponseEntity<ApiResponse<ServiceRequestResponse>> createRequest(
      @Valid @RequestBody ServiceRequestCreateDto request) {
    ServiceRequestResponse response = serviceRequestService.createRequest(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Get my submitted service requests (Customer)")
  public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> getMyRequests(Pageable pageable) {
    Page<ServiceRequestResponse> response = serviceRequestService.getMyRequests(pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get service request by ID")
  public ResponseEntity<ApiResponse<ServiceRequestResponse>> getRequestById(@PathVariable("id") Long id) {
    ServiceRequestResponse response = serviceRequestService.getRequestById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Search service tickets queue (Staff / Admin)")
  public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> searchRequests(
      @RequestParam(value = "status", required = false) ServiceRequestStatus status,
      @RequestParam(value = "type", required = false) ServiceRequestType type,
      Pageable pageable) {
    Page<ServiceRequestResponse> response = serviceRequestService.searchRequests(status, type, pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/assign")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Assign ticket to current staff member (Staff)")
  public ResponseEntity<ApiResponse<ServiceRequestResponse>> assignToMe(@PathVariable("id") Long id) {
    ServiceRequestResponse response = serviceRequestService.assignToMe(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/resolve")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Resolve or reject service ticket (Staff)")
  public ResponseEntity<ApiResponse<ServiceRequestResponse>> resolveRequest(
      @PathVariable("id") Long id,
      @Valid @RequestBody ServiceRequestResolveDto request) {
    ServiceRequestResponse response = serviceRequestService.resolveRequest(id, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
