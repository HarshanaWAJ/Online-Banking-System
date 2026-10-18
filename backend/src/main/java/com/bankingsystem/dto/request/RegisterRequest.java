package com.bankingsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registration request payload for new customers.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    String password,

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name cannot exceed 200 characters")
    String fullName,

    @NotBlank(message = "NIC is required")
    @Size(min = 9, max = 20, message = "NIC must be between 9 and 20 characters")
    String nic,

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phone,

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    String address,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth) {}
