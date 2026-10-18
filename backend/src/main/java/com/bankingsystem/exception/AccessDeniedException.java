package com.bankingsystem.exception;

/**
 * Thrown when an authenticated user attempts an operation on a resource
 * they do not have sufficient permissions to access.
 */
public class AccessDeniedException extends RuntimeException {

  public AccessDeniedException(String message) {
    super(message);
  }

  public AccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}
