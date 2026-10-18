package com.bankingsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials request payload.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,

    @NotBlank(message = "Password is required")
    String password) {}
