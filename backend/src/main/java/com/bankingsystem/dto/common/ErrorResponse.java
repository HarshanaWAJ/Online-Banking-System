package com.bankingsystem.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Standard error response envelope.
 *
 * @param code machine-readable error code
 * @param message human-readable error description
 * @param details optional list of field-level errors
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    String code,
    String message,
    List<FieldErrorDetail> details) {

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message, List.of());
  }

  public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
    return new ErrorResponse(code, message, details != null ? details : List.of());
  }
}
