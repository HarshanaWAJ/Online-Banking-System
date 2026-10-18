package com.bankingsystem.enums;

import java.util.Set;

/**
 * Bank account lifecycle states and validation for valid status transitions.
 */
public enum AccountStatus {
  PENDING,
  ACTIVE,
  FROZEN,
  CLOSED;

  public boolean canTransitionTo(AccountStatus next) {
    if (this == next) {
      return true;
    }
    return switch (this) {
      case PENDING -> next == ACTIVE || next == CLOSED;
      case ACTIVE -> next == FROZEN || next == CLOSED;
      case FROZEN -> next == ACTIVE || next == CLOSED;
      case CLOSED -> false; // Terminal state
    };
  }
}
