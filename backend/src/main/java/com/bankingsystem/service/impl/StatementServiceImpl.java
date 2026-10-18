package com.bankingsystem.service.impl;

import com.bankingsystem.dto.response.LedgerEntryResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.Role;
import com.bankingsystem.exception.AccessDeniedException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.StatementService;
import com.bankingsystem.util.CsvUtil;
import com.bankingsystem.util.PdfStatementWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * StatementServiceImpl
 * ============================================================================
 * 
 * Core banking service responsible for:
 * 1. Querying chronological append-only ledger entries for any bank account.
 * 2. Enforcing strict multi-tenant customer ownership and access control.
 * 3. Calculating mathematically accurate opening and closing balances for arbitrary date windows.
 * 4. Generating CSV statements with security formula injection neutralization (RFC 4180).
 * 5. Generating formal PDF account statements utilizing iText/PDF engine.
 * ============================================================================
 */
@Service
public class StatementServiceImpl implements StatementService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final BankAccountRepository bankAccountRepository;
  private final OwnershipGuard ownershipGuard;

  public StatementServiceImpl(
      LedgerEntryRepository ledgerEntryRepository,
      BankAccountRepository bankAccountRepository,
      OwnershipGuard ownershipGuard) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.bankAccountRepository = bankAccountRepository;
    this.ownershipGuard = ownershipGuard;
  }

  /**
   * Retrieves a paginated list of immutable ledger entries for an account.
   * 
   * @param accountId Target bank account ID
   * @param from      Optional start date/time (defaults to Instant.EPOCH if null for all-time query)
   * @param to        Optional end date/time (defaults to current time + 1 day to cover clock variations)
   * @param pageable  Spring Data pagination and sorting specification
   * @return Paginated LedgerEntryResponse objects ordered by newest first (DESC)
   */
  @Override
  @Transactional(readOnly = true)
  public Page<LedgerEntryResponse> getAccountTransactions(
      Long accountId,
      Instant from,
      Instant to,
      Pageable pageable) {
    // Step 1: Ensure user is authorized to inspect this account
    BankAccount account = getAccountAndValidateAccess(accountId);

    // Step 2: Establish date boundaries. If no 'from' provided, return all history up to 'to'.
    Instant effectiveTo = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);
    Instant effectiveFrom = from != null ? from : Instant.EPOCH;

    // Step 3: Fetch entries indexed by (accountId, createdAt)
    return ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(account.getId(), effectiveFrom, effectiveTo, pageable)
        .map(this::mapToResponse);
  }

  /**
   * Generates a downloadable CSV statement for the specified account and date range.
   * Includes header metadata (account number, holder name, period, opening/closing balance)
   * and tabular transactional rows.
   * 
   * @param accountId Target bank account ID
   * @param from      Start date boundary (defaults to 30 days prior if not specified)
   * @param to        End date boundary (defaults to now if not specified)
   * @return UTF-8 encoded CSV bytes
   */
  @Override
  @Transactional(readOnly = true)
  public byte[] exportStatementCsv(Long accountId, Instant from, Instant to) {
    // Step 1: Validate account ownership and permissions
    BankAccount account = getAccountAndValidateAccess(accountId);

    // Step 2: Determine date range for export (default: last 30 days)
    Instant effectiveTo = to != null ? to : Instant.now();
    Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

    // Step 3: Retrieve transactions in chronological ASCENDING order for statement accounting
    List<LedgerEntry> entries = ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), effectiveFrom, effectiveTo);

    // Step 4: Calculate opening and closing balance
    BigDecimal openingBalance = calculateOpeningBalance(account, effectiveFrom, entries);
    BigDecimal closingBalance = entries.isEmpty()
        ? openingBalance
        : entries.get(entries.size() - 1).getBalanceAfter();

    // Step 5: Write CSV with formula injection defense
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      // Statement Header Summary
      csvPrinter.printRecord("ACCOUNT STATEMENT", "");
      csvPrinter.printRecord("Account Holder", CsvUtil.neutralizeFormula(account.getCustomer().getFullName()));
      csvPrinter.printRecord("Account Number", CsvUtil.neutralizeFormula(account.getAccountNumber()));
      csvPrinter.printRecord("Account Type", account.getType().name());
      csvPrinter.printRecord("Currency", account.getCurrency());
      csvPrinter.printRecord("Statement Period From", effectiveFrom.toString());
      csvPrinter.printRecord("Statement Period To", effectiveTo.toString());
      csvPrinter.printRecord("Opening Balance", openingBalance.toPlainString());
      csvPrinter.printRecord("Closing Balance", closingBalance.toPlainString());
      csvPrinter.println();

      // Table Header Row
      csvPrinter.printRecord(
          "Transaction Date",
          "Reference",
          "Operation Type",
          "Direction",
          "Amount",
          "Balance After",
          "Description"
      );

      // Detail Rows
      for (LedgerEntry e : entries) {
        csvPrinter.printRecord(
            e.getCreatedAt().toString(),
            CsvUtil.neutralizeFormula(e.getReference()),
            e.getOperationType().name(),
            e.getDirection().name(),
            e.getAmount().toPlainString(),
            e.getBalanceAfter().toPlainString(),
            CsvUtil.neutralizeFormula(e.getDescription())
        );
      }

      csvPrinter.flush();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate CSV statement: " + ex.getMessage(), ex);
    }
  }

  /**
   * Generates an official bank PDF statement document with institutional styling,
   * verification reference, date ranges, and balance summary.
   * 
   * @param accountId Target bank account ID
   * @param from      Start date boundary
   * @param to        End date boundary
   * @return PDF binary byte array
   */
  @Override
  @Transactional(readOnly = true)
  public byte[] exportStatementPdf(Long accountId, Instant from, Instant to) {
    // Step 1: Validate account access
    BankAccount account = getAccountAndValidateAccess(accountId);

    // Step 2: Establish date boundaries (defaults to past 30 days)
    Instant effectiveTo = to != null ? to : Instant.now();
    Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

    // Step 3: Fetch transactions in chronological ASCENDING order
    List<LedgerEntry> entries = ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), effectiveFrom, effectiveTo);

    // Step 4: Calculate balance boundaries
    BigDecimal openingBalance = calculateOpeningBalance(account, effectiveFrom, entries);
    BigDecimal closingBalance = entries.isEmpty()
        ? openingBalance
        : entries.get(entries.size() - 1).getBalanceAfter();

    // Step 5: Render PDF document
    return PdfStatementWriter.generateStatementPdf(
        account,
        entries,
        openingBalance,
        closingBalance,
        effectiveFrom,
        effectiveTo
    );
  }

  /**
   * Security & Authorization Guard:
   * Ensures that if the caller is a CUSTOMER, they can ONLY access their own account.
   * If a customer attempts to query another user's account ID, a ResourceNotFoundException
   * is thrown to avoid leaking the existence of other accounts.
   * Staff and Admin roles are granted viewing access for auditing and operations.
   */
  private BankAccount getAccountAndValidateAccess(Long accountId) {
    AppUser user = ownershipGuard.getCurrentUser();
    BankAccount account = bankAccountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (user.getRole() == Role.CUSTOMER) {
      if (!account.getCustomer().getUser().getId().equals(user.getId())) {
        // Return 404 to avoid leaking existence of other customer accounts (OWASP ASVS compliance)
        throw new ResourceNotFoundException("BankAccount", accountId);
      }
    }
    return account;
  }

  /**
   * Calculates the exact opening balance at the start of the period.
   * Looks up the last ledger entry strictly prior to 'from'.
   * If no previous entry exists, the opening balance is 0.00.
   */
  private BigDecimal calculateOpeningBalance(BankAccount account, Instant from, List<LedgerEntry> entries) {
    return ledgerEntryRepository
        .findFirstByAccountIdAndCreatedAtLessThanOrderByCreatedAtDesc(account.getId(), from)
        .map(LedgerEntry::getBalanceAfter)
        .orElse(BigDecimal.ZERO.setScale(2));
  }

  /**
   * Transforms internal JPA LedgerEntry entity into external client DTO.
   */
  private LedgerEntryResponse mapToResponse(LedgerEntry e) {
    return new LedgerEntryResponse(
        e.getId(),
        e.getAccount().getId(),
        e.getAccount().getAccountNumber(),
        e.getDirection(),
        e.getAmount(),
        e.getBalanceAfter(),
        e.getOperationType(),
        e.getReference(),
        e.getDescription(),
        e.getCompensatesEntryId(),
        e.getCreatedAt()
    );
  }
}
