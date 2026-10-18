package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.REPORTS)
@Tag(name = "Reports & Analytics", description = "System metrics and administrative CSV exports")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/overview")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Get system overview metrics (Staff / Admin)")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
    Map<String, Object> stats = reportService.getSystemOverviewStats();
    return ResponseEntity.ok(ApiResponse.ok(stats));
  }

  @GetMapping("/export/accounts")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Export accounts list as CSV (Staff / Admin)")
  public ResponseEntity<byte[]> exportAccountsCsv() {
    byte[] csvData = reportService.exportAccountsReportCsv();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts_report.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csvData);
  }

  @GetMapping("/export/transactions")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  @Operation(summary = "Export transactions audit log as CSV (Staff / Admin)")
  public ResponseEntity<byte[]> exportTransactionsCsv(
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to) {
    byte[] csvData = reportService.exportTransactionsReportCsv(from, to);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions_report.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csvData);
  }
}
