package com.bankingsystem.dto.common;

/**
 * Details of a single field-level validation error.
 *
 * @param field field name
 * @param message validation error message
 */
public record FieldErrorDetail(String field, String message) {}
