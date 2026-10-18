package com.bankingsystem.dto.response;

import com.bankingsystem.enums.Role;
import com.bankingsystem.enums.StaffPermission;
import java.util.Set;

/**
 * User identity and authorization response.
 */
public record UserResponse(
    Long id,
    String email,
    Role role,
    boolean active,
    Set<StaffPermission> permissions) {}
