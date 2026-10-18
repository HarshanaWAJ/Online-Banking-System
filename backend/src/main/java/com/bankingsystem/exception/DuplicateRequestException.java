package com.bankingsystem.exception;

/**
 * Thrown when an idempotent submission requestKey has already been processed.
 */
public class DuplicateRequestException extends RuntimeException {

  private final String requestKey;

  public DuplicateRequestException(String requestKey) {
    super(String.format("Request with key '%s' has already been submitted or is being processed", requestKey));
    this.requestKey = requestKey;
  }

  public String getRequestKey() {
    return requestKey;
  }
}
