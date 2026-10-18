package com.bankingsystem.dto.response;

/**
 * Beneficiary details response.
 */
public record BeneficiaryResponse(
    Long id,
    String name,
    String accountNumber,
    String nickname) {}
