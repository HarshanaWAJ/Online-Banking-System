package com.bankingsystem.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generator for human-readable transaction and reference codes.
 */
public final class ReferenceGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private ReferenceGenerator() {}

  public static String generateTransferReference() {
    return generateReference("TRF");
  }

  public static String generateBillPaymentReference() {
    return generateReference("BIL");
  }

  public static String generateCashOperationReference() {
    return generateReference("CSH");
  }

  public static String generateCompensationReference() {
    return generateReference("CMP");
  }

  private static String generateReference(String prefix) {
    String dateStr = LocalDate.now().format(DATE_FORMATTER);
    int randomPart = 100000 + RANDOM.nextInt(900000);
    return String.format("%s-%s-%06d", prefix, dateStr, randomPart);
  }
}
