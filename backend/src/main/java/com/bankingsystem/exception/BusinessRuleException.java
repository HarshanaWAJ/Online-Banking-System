package com.bankingsystem.exception;

/**
 * Thrown when an operation violates banking domain rules
 * (e.g., insufficient funds, illegal account status transition, transfer to self).
 */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }

  public BusinessRuleException(String message, Throwable cause) {
    super(message, cause);
  }
}
