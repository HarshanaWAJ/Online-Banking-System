package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.response.LedgerEntryResponse;
import com.bankingsystem.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for querying account transactions and generating statements (CSV, PDF).
 */
@RestController
@Tag(name = "Transactions & Statements", description = "Query account ledger history and export statements")
public class TransactionController {

  private final StatementService statementService;

  public TransactionController(StatementService statementService) {
    this.statementService = statementService;
  }

  @GetMapping(ApiPaths.ACCOUNTS + "/{id}/transactions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get account transactions (Customer/Staff)", description = "Retrieves paginated ledger entries for the specified account")
  public ResponseEntity<ApiResponse<java.util.List<LedgerEntryResponse>>> getAccountTransactions(
      @PathVariable("id") Long accountId,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to,
      Pageable pageable) {
    Page<LedgerEntryResponse> page = statementService.getAccountTransactions(accountId, from, to, pageable);
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), com.bankingsystem.dto.common.PageMeta.of(page)));
  }

  @GetMapping(ApiPaths.ACCOUNTS + "/{id}/statement")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Export account statement (CSV or PDF)", description = "Exports statement file for specified date range")
  public ResponseEntity<byte[]> exportStatement(
      @PathVariable("id") Long accountId,
      @RequestParam(value = "format", defaultValue = "pdf") String format,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to) {

    if ("csv".equalsIgnoreCase(format)) {
      byte[] csvBytes = statementService.exportStatementCsv(accountId, from, to);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement_" + accountId + ".csv\"")
          .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
          .body(csvBytes);
    } else {
      byte[] pdfBytes = statementService.exportStatementPdf(accountId, from, to);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement_" + accountId + ".pdf\"")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);
    }
  }
}
