package com.bankingsystem.dto.response;

import java.time.LocalDate;

/**
 * Customer profile detail response.
 */
public record CustomerResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String nic,
    String phone,
    String address,
    LocalDate dateOfBirth,
    boolean active) {}
