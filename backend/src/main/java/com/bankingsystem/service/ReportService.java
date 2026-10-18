package com.bankingsystem.service;

import java.time.Instant;
import java.util.Map;

public interface ReportService {

  Map<String, Object> getSystemOverviewStats();

  byte[] exportTransactionsReportCsv(Instant from, Instant to);

  byte[] exportAccountsReportCsv();
}
