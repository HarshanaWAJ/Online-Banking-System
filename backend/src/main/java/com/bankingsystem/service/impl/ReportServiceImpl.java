package com.bankingsystem.service.impl;

import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.entity.LedgerEntry;
import com.bankingsystem.enums.AccountStatus;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.LedgerEntryRepository;
import com.bankingsystem.repository.ServiceRequestRepository;
import com.bankingsystem.service.ReportService;
import com.bankingsystem.util.CsvUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

  private final BankAccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final ServiceRequestRepository serviceRequestRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  public ReportServiceImpl(
      BankAccountRepository accountRepository,
      CustomerRepository customerRepository,
      ServiceRequestRepository serviceRequestRepository,
      LedgerEntryRepository ledgerEntryRepository) {
    this.accountRepository = accountRepository;
    this.customerRepository = customerRepository;
    this.serviceRequestRepository = serviceRequestRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> getSystemOverviewStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalCustomers", customerRepository.count());
    stats.put("totalAccounts", accountRepository.count());
    stats.put("pendingAccountRequests", accountRepository.countByStatus(AccountStatus.PENDING));
    stats.put("totalServiceTickets", serviceRequestRepository.count());
    return stats;
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportTransactionsReportCsv(Instant from, Instant to) {
    Instant effectiveTo = to != null ? to : Instant.now();
    Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      csvPrinter.printRecord("BANK TRANSACTION AUDIT REPORT", "");
      csvPrinter.printRecord("From", effectiveFrom.toString());
      csvPrinter.printRecord("To", effectiveTo.toString());
      csvPrinter.println();

      csvPrinter.printRecord("ID", "Timestamp", "Account", "Operation", "Direction", "Amount", "Balance After", "Reference", "Description");
      csvPrinter.flush();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate transactions report CSV: " + ex.getMessage(), ex);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportAccountsReportCsv() {
    List<BankAccount> accounts = accountRepository.findAll();

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      csvPrinter.printRecord("BANK ACCOUNTS STATUS REPORT", "");
      csvPrinter.printRecord("Generated At", Instant.now().toString());
      csvPrinter.println();

      csvPrinter.printRecord("ID", "Account Number", "Customer Name", "Type", "Status", "Currency", "Balance", "Requested At");

      for (BankAccount acc : accounts) {
        csvPrinter.printRecord(
            acc.getId(),
            CsvUtil.neutralizeFormula(acc.getAccountNumber() != null ? acc.getAccountNumber() : "PENDING"),
            CsvUtil.neutralizeFormula(acc.getCustomer() != null ? acc.getCustomer().getFullName() : "N/A"),
            acc.getType() != null ? acc.getType().name() : "",
            acc.getStatus() != null ? acc.getStatus().name() : "",
            acc.getCurrency(),
            acc.getBalance() != null ? acc.getBalance().toPlainString() : "0.00",
            acc.getRequestedAt() != null ? acc.getRequestedAt().toString() : ""
        );
      }

      csvPrinter.flush();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate accounts report CSV: " + ex.getMessage(), ex);
    }
  }
}
