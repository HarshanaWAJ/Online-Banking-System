package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.CreateStaffUserRequest;
import com.bankingsystem.dto.request.UpdateStaffPermissionsRequest;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.AppUserRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AdminUserService;
import com.bankingsystem.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;

  public AdminUserServiceImpl(
      AppUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      OwnershipGuard ownershipGuard,
      AuditService auditService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserResponse> listUsers(Role role, Pageable pageable) {
    if (role != null) {
      return userRepository.findAll(pageable)
          .map(this::mapToResponse); // or custom query
    }
    return userRepository.findAll(pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional
  public UserResponse createStaffUser(CreateStaffUserRequest request) {
    AppUser admin = ownershipGuard.getCurrentUser();
    String email = request.email().trim().toLowerCase();

    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessRuleException("A user with email " + email + " already exists.");
    }

    if (request.role() == Role.CUSTOMER) {
      throw new BusinessRuleException("Customer users must register through the customer portal.");
    }

    AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.role());
    if (request.permissions() != null) {
      user.setPermissions(request.permissions());
    }

    user = userRepository.save(user);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "USER_CREATED",
        "AppUser",
        user.getId(),
        AuditOutcome.SUCCESS,
        "Created staff user " + user.getEmail() + " (" + user.getRole() + ")",
        null
    );

    return mapToResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateUserStatus(Long userId, boolean active) {
    AppUser admin = ownershipGuard.getCurrentUser();
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

    if (user.getId().equals(admin.getId())) {
      throw new BusinessRuleException("Admins cannot deactivate their own user account.");
    }

    user.setActive(active);
    user = userRepository.save(user);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "USER_STATUS_UPDATED",
        "AppUser",
        user.getId(),
        AuditOutcome.SUCCESS,
        "Set user " + user.getEmail() + " active status to " + active,
        null
    );

    return mapToResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateStaffPermissions(Long userId, UpdateStaffPermissionsRequest request) {
    AppUser admin = ownershipGuard.getCurrentUser();
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

    user.setPermissions(request.permissions());
    user = userRepository.save(user);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "USER_PERMISSIONS_UPDATED",
        "AppUser",
        user.getId(),
        AuditOutcome.SUCCESS,
        "Updated permissions for staff user " + user.getEmail(),
        null
    );

    return mapToResponse(user);
  }

  private UserResponse mapToResponse(AppUser u) {
    return new UserResponse(
        u.getId(),
        u.getEmail(),
        u.getRole(),
        u.isActive(),
        u.getPermissions()
    );
  }
}
