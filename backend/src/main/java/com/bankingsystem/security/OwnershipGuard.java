package com.bankingsystem.security;

import com.bankingsystem.entity.AppUser;
import com.bankingsystem.exception.AccessDeniedException;
import com.bankingsystem.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to resolve current authenticated user and enforce resource ownership checks.
 */
@Component
public class OwnershipGuard {

  private final AppUserRepository userRepository;

  public OwnershipGuard(AppUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public AppUser getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("User is not authenticated");
    }
    String email = auth.getName();
    return userRepository
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
  }

  public Long getCurrentUserId() {
    return getCurrentUser().getId();
  }

  public void verifyOwnership(Long ownerId) {
    Long currentId = getCurrentUserId();
    if (!currentId.equals(ownerId)) {
      throw new AccessDeniedException("You do not own this resource");
    }
  }
}
