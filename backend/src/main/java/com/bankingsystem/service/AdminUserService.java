package com.bankingsystem.service;

import com.bankingsystem.dto.request.CreateStaffUserRequest;
import com.bankingsystem.dto.request.UpdateStaffPermissionsRequest;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

  Page<UserResponse> listUsers(Role role, Pageable pageable);

  UserResponse createStaffUser(CreateStaffUserRequest request);

  UserResponse updateUserStatus(Long userId, boolean active);

  UserResponse updateStaffPermissions(Long userId, UpdateStaffPermissionsRequest request);
}
