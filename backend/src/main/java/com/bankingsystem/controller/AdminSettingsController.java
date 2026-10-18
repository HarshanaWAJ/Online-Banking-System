package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.admin.SystemSettingResponse;
import com.bankingsystem.dto.admin.UpdateSettingsRequest;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system settings management.
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_SETTINGS)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Settings", description = "Endpoints for administering system limits, configurations, and maintenance")
public class AdminSettingsController {

  private final AdminSettingsService adminSettingsService;
  private final OwnershipGuard ownershipGuard;

  public AdminSettingsController(
      AdminSettingsService adminSettingsService,
      OwnershipGuard ownershipGuard) {
    this.adminSettingsService = adminSettingsService;
    this.ownershipGuard = ownershipGuard;
  }

  @GetMapping
  @Operation(summary = "Get all system settings")
  public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> getSettings() {
    List<SystemSettingResponse> settings = adminSettingsService.getAllSettings();
    return ResponseEntity.ok(ApiResponse.ok(settings));
  }

  @PutMapping
  @Operation(summary = "Update system settings")
  public ResponseEntity<ApiResponse<Void>> updateSettings(
      @Valid @RequestBody UpdateSettingsRequest request) {
    Long adminUserId = ownershipGuard.getCurrentUserId();
    adminSettingsService.updateSettings(request, adminUserId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
