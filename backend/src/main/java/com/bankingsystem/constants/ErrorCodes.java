package com.bankingsystem.constants;

/**
 * Standard error codes returned in API error envelopes.
 */
public final class ErrorCodes {

  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String ACCESS_DENIED = "ACCESS_DENIED";
  public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
  public static final String DUPLICATE_REQUEST = "DUPLICATE_REQUEST";
  public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
  public static final String RATE_LIMITED = "RATE_LIMITED";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private ErrorCodes() {
    // Utility class — prevent instantiation
  }
}
