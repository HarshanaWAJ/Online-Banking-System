package com.bankingsystem.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response envelope.
 *
 * @param success whether the operation succeeded
 * @param data payload on success (null on failure)
 * @param meta pagination metadata (only present for paginated lists)
 * @param error error details on failure (null on success)
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    PageMeta meta,
    ErrorResponse error) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> ok(T data, PageMeta meta) {
    return new ApiResponse<>(true, data, meta, null);
  }

  public static <T> ApiResponse<T> error(ErrorResponse error) {
    return new ApiResponse<>(false, null, null, error);
  }
}
