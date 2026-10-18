package com.bankingsystem.dto.common;

import org.springframework.data.domain.Page;

/**
 * Standard pagination metadata record.
 *
 * @param page current zero-based page index
 * @param size page size
 * @param totalElements total number of records
 * @param totalPages total number of pages
 */
public record PageMeta(
    int page,
    int size,
    long totalElements,
    int totalPages) {

  public static PageMeta of(Page<?> springPage) {
    return new PageMeta(
        springPage.getNumber(),
        springPage.getSize(),
        springPage.getTotalElements(),
        springPage.getTotalPages());
  }
}
