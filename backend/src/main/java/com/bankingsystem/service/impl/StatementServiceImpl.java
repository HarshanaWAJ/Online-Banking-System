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
 * Implementation of StatementService for querying ledger transactions,
 * generating CSV and PDF account statements.
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

  @Override
  @Transactional(readOnly = true)
  public Page<LedgerEntryResponse> getAccountTransactions(
      Long accountId,
      Instant from,
      Instant to,
      Pageable pageable) {
    BankAccount account = getAccountAndValidateAccess(accountId);

    Instant effectiveTo = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);
    Instant effectiveFrom = from != null ? from : Instant.EPOCH;

    return ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(account.getId(), effectiveFrom, effectiveTo, pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportStatementCsv(Long accountId, Instant from, Instant to) {
    BankAccount account = getAccountAndValidateAccess(accountId);

    Instant effectiveTo = to != null ? to : Instant.now();
    Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

    List<LedgerEntry> entries = ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), effectiveFrom, effectiveTo);

    BigDecimal openingBalance = calculateOpeningBalance(account, effectiveFrom, entries);
    BigDecimal closingBalance = entries.isEmpty()
        ? openingBalance
        : entries.get(entries.size() - 1).getBalanceAfter();

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      // Header block
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

      // Table columns
      csvPrinter.printRecord(
          "Transaction Date",
          "Reference",
          "Operation Type",
          "Direction",
          "Amount",
          "Balance After",
          "Description"
      );

      // Data rows
      for (LedgerEntry entry : entries) {
        csvPrinter.printRecord(
            entry.getCreatedAt().toString(),
            CsvUtil.neutralizeFormula(entry.getReference()),
            entry.getOperationType().name(),
            entry.getDirection().name(),
            entry.getAmount().toPlainString(),
            entry.getBalanceAfter().toPlainString(),
            CsvUtil.neutralizeFormula(entry.getDescription())
        );
      }

      csvPrinter.flush();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate CSV statement: " + ex.getMessage(), ex);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportStatementPdf(Long accountId, Instant from, Instant to) {
    BankAccount account = getAccountAndValidateAccess(accountId);

    Instant effectiveTo = to != null ? to : Instant.now();
    Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

    List<LedgerEntry> entries = ledgerEntryRepository
        .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), effectiveFrom, effectiveTo);

    BigDecimal openingBalance = calculateOpeningBalance(account, effectiveFrom, entries);
    BigDecimal closingBalance = entries.isEmpty()
        ? openingBalance
        : entries.get(entries.size() - 1).getBalanceAfter();

    return PdfStatementWriter.generateStatementPdf(
        account,
        entries,
        openingBalance,
        closingBalance,
        effectiveFrom,
        effectiveTo
    );
  }

  private BankAccount getAccountAndValidateAccess(Long accountId) {
    AppUser user = ownershipGuard.getCurrentUser();
    BankAccount account = bankAccountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("BankAccount", accountId));

    if (user.getRole() == Role.CUSTOMER) {
      if (!account.getCustomer().getUser().getId().equals(user.getId())) {
        // Return 404 to avoid leaking existence of other customer accounts
        throw new ResourceNotFoundException("BankAccount", accountId);
      }
    }
    return account;
  }

  private BigDecimal calculateOpeningBalance(BankAccount account, Instant from, List<LedgerEntry> entries) {
    return ledgerEntryRepository
        .findFirstByAccountIdAndCreatedAtLessThanOrderByCreatedAtDesc(account.getId(), from)
        .map(LedgerEntry::getBalanceAfter)
        .orElse(BigDecimal.ZERO.setScale(2));
  }

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
