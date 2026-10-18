package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.response.LedgerEntryResponse;
import com.bankingsystem.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
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
 * ============================================================================
 * TransactionController
 * ============================================================================
 * 
 * REST Controller exposing account transaction queries and file downloads.
 * 
 * Endpoints:
 * 1. GET /api/accounts/{id}/transactions
 *    - Paginated ledger history with optional ISO-8601 date range filters (from, to).
 *    - Encapsulated inside standardized ApiResponse<List<LedgerEntryResponse>>
 *      accompanied by PageMeta pagination metadata.
 * 
 * 2. GET /api/accounts/{id}/statement
 *    - On-demand statement generation in CSV or PDF format.
 *    - Sets Content-Disposition headers for direct browser downloads.
 * ============================================================================
 */
@RestController
@Tag(name = "Transactions & Statements", description = "Query account ledger history and export statements")
public class TransactionController {

  private final StatementService statementService;

  public TransactionController(StatementService statementService) {
    this.statementService = statementService;
  }

  /**
   * Retrieves paginated transaction records for an account.
   * 
   * @param accountId Bank account database identifier
   * @param from      Optional start date/time (ISO-8601)
   * @param to        Optional end date/time (ISO-8601)
   * @param pageable  Page specification (page index, size, sorting)
   * @return Standardized ApiResponse containing the ledger entry list and pagination metadata
   */
  @GetMapping(ApiPaths.ACCOUNTS + "/{id}/transactions")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Get account transactions (Customer/Staff)",
      description = "Retrieves paginated ledger entries for the specified account with running balance snapshots"
  )
  public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getAccountTransactions(
      @PathVariable("id") Long accountId,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to,
      Pageable pageable) {
    // Invoke statement service to fetch data with security ownership check
    Page<LedgerEntryResponse> page = statementService.getAccountTransactions(accountId, from, to, pageable);

    // Return standardized ApiResponse with content and page metadata (totalPages, totalElements, etc.)
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), com.bankingsystem.dto.common.PageMeta.of(page)));
  }

  /**
   * Generates and streams an account statement document in CSV or PDF format.
   * 
   * @param accountId Target bank account ID
   * @param format    Export format: 'pdf' (default) or 'csv'
   * @param from      Optional filter start date/time
   * @param to        Optional filter end date/time
   * @return Binary file attachment response with appropriate MIME type and Content-Disposition
   */
  @GetMapping(ApiPaths.ACCOUNTS + "/{id}/statement")
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Export account statement (CSV or PDF)",
      description = "Generates and downloads statement file for the specified date range"
  )
  public ResponseEntity<byte[]> exportStatement(
      @PathVariable("id") Long accountId,
      @RequestParam(value = "format", defaultValue = "pdf") String format,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to) {

    if ("csv".equalsIgnoreCase(format)) {
      // 1. Generate RFC 4180 compliant CSV statement
      byte[] csvBytes = statementService.exportStatementCsv(accountId, from, to);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement_" + accountId + ".csv\"")
          .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
          .body(csvBytes);
    } else {
      // 2. Generate official PDF statement
      byte[] pdfBytes = statementService.exportStatementPdf(accountId, from, to);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement_" + accountId + ".pdf\"")
          .contentType(MediaType.APPLICATION_PDF)
          .body(pdfBytes);
    }
  }
}
