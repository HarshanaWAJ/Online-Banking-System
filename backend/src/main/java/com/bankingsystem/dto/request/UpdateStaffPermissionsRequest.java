package com.bankingsystem.dto.request;

import com.bankingsystem.enums.StaffPermission;
import java.util.Set;

public record UpdateStaffPermissionsRequest(
    Set<StaffPermission> permissions
) {}
