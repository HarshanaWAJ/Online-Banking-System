package com.bankingsystem.exception;

import com.bankingsystem.constants.ErrorCodes;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.common.ErrorResponse;
import com.bankingsystem.dto.common.FieldErrorDetail;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler for all REST controllers.
 * Ensures consistent ApiResponse error envelope formatting and avoids leaking internal stack traces or SQL details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
        .collect(Collectors.toList());

    log.warn("Validation error on request: {}", details);
    ErrorResponse error = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, "Invalid input data", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
    List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
        .map(cv -> new FieldErrorDetail(cv.getPropertyPath().toString(), cv.getMessage()))
        .collect(Collectors.toList());

    log.warn("Constraint violation: {}", details);
    ErrorResponse error = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, "Constraint violation", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
    log.warn("Business validation failed: {}", ex.getMessage());
    ErrorResponse error = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, ex.getMessage(), ex.getErrors());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
    log.info("Resource not found: {}", ex.getMessage());
    ErrorResponse error = ErrorResponse.of(ErrorCodes.NOT_FOUND, ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException ex) {
    log.warn("Business rule violation: {}", ex.getMessage());
    ErrorResponse error = ErrorResponse.of(ErrorCodes.BUSINESS_RULE_VIOLATION, ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler(DuplicateRequestException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateRequest(DuplicateRequestException ex) {
    log.warn("Duplicate request detected: key={}", ex.getRequestKey());
    ErrorResponse error = ErrorResponse.of(ErrorCodes.DUPLICATE_REQUEST, ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler({
      AccessDeniedException.class,
      org.springframework.security.access.AccessDeniedException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
    log.warn("Access denied: {}", ex.getMessage());
    ErrorResponse error = ErrorResponse.of(ErrorCodes.ACCESS_DENIED, "Access denied");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());
    String msg = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : "Invalid credentials or session expired";
    ErrorResponse error = ErrorResponse.of(ErrorCodes.UNAUTHENTICATED, msg);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    ErrorResponse error = ErrorResponse.of(
        ErrorCodes.BUSINESS_RULE_VIOLATION,
        "The operation conflicts with existing data or violates data integrity constraints.");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unhandled server exception", ex);
    ErrorResponse error = ErrorResponse.of(
        ErrorCodes.INTERNAL_ERROR,
        "An unexpected error occurred. Please try again later.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
  }
}
