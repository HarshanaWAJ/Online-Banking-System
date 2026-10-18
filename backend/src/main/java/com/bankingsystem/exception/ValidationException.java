package com.bankingsystem.exception;

import com.bankingsystem.dto.common.FieldErrorDetail;
import java.util.List;

/**
 * Thrown when custom program-level validation fails.
 */
public class ValidationException extends RuntimeException {

  private final List<FieldErrorDetail> errors;

  public ValidationException(String message, List<FieldErrorDetail> errors) {
    super(message);
    this.errors = errors != null ? errors : List.of();
  }

  public ValidationException(List<FieldErrorDetail> errors) {
    super("Validation failed");
    this.errors = errors != null ? errors : List.of();
  }

  public List<FieldErrorDetail> getErrors() {
    return errors;
  }
}
