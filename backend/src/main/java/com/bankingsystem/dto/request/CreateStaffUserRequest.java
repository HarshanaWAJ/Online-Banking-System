package com.bankingsystem.dto.request;

import com.bankingsystem.enums.Role;
import com.bankingsystem.enums.StaffPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateStaffUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotNull(message = "Role is required (STAFF or ADMIN)")
    Role role,

    Set<StaffPermission> permissions
) {}
