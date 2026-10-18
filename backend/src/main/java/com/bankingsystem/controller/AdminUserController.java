package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.CreateStaffUserRequest;
import com.bankingsystem.dto.request.UpdateStaffPermissionsRequest;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.enums.Role;
import com.bankingsystem.service.AdminUserService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_USERS)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Endpoints for managing staff and admin users and permissions")
public class AdminUserController {

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  @Operation(summary = "List all system users (Admin)")
  public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
      @RequestParam(value = "role", required = false) Role role,
      Pageable pageable) {
    Page<UserResponse> response = adminUserService.listUsers(role, pageable);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping
  @Operation(summary = "Create a new staff or admin user (Admin)")
  public ResponseEntity<ApiResponse<UserResponse>> createStaffUser(
      @Valid @RequestBody CreateStaffUserRequest request) {
    UserResponse response = adminUserService.createStaffUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Activate or deactivate a user (Admin)")
  public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
      @PathVariable("id") Long userId,
      @RequestParam("active") boolean active) {
    UserResponse response = adminUserService.updateUserStatus(userId, active);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PutMapping("/{id}/permissions")
  @Operation(summary = "Update granular staff permissions (Admin)")
  public ResponseEntity<ApiResponse<UserResponse>> updateStaffPermissions(
      @PathVariable("id") Long userId,
      @RequestBody UpdateStaffPermissionsRequest request) {
    UserResponse response = adminUserService.updateStaffPermissions(userId, request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
