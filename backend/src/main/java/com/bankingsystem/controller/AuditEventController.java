package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.common.PageMeta;
import com.bankingsystem.entity.AuditEvent;
import com.bankingsystem.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving privileged audit trail events.
 */
@RestController
@RequestMapping(ApiPaths.AUDIT_EVENTS)
@Tag(name = "Audit Events", description = "Endpoints for viewing system audit history (Admin)")
public class AuditEventController {

  private final AuditService auditService;

  public AuditEventController(AuditService auditService) {
    this.auditService = auditService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Search system audit trail (Admin)")
  @SuppressWarnings("unchecked")
  public ResponseEntity<ApiResponse<List<AuditEvent>>> getAuditEvents(
      @RequestParam(name = "actorId", required = false) Long actorId,
      @RequestParam(name = "action", required = false) String action,
      @RequestParam(name = "from", required = false) Instant from,
      @RequestParam(name = "to", required = false) Instant to,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

    Page<AuditEvent> page = (Page<AuditEvent>) auditService.searchAuditEvents(actorId, action, from, to, pageable);
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), PageMeta.of(page)));
  }
}
