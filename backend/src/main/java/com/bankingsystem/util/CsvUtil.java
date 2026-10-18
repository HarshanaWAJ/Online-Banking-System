package com.bankingsystem.util;

/**
 * Utility for sanitizing CSV cell values to prevent CSV formula injection attacks
 * (CSV Injection / Formula Injection).
 */
public final class CsvUtil {

  private CsvUtil() {}

  /**
   * Neutralizes formula injection by prepending a single quote (') if the value begins
   * with unsafe characters: =, +, -, @, Tab, or Carriage Return.
   */
  public static String neutralizeFormula(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }
    char firstChar = input.charAt(0);
    if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@'
        || firstChar == '\t' || firstChar == '\r') {
      return "'" + input;
    }
    return input;
  }
}
