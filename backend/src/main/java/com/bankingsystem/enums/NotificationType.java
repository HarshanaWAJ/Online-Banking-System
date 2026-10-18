package com.bankingsystem.enums;

/**
 * Type of notification sent to customers or staff.
 */
public enum NotificationType {
  ACCOUNT_APPROVED,
  ACCOUNT_REJECTED,
  ACCOUNT_FROZEN,
  ACCOUNT_UNFROZEN,
  ACCOUNT_CLOSED,
  TRANSFER_SENT,
  TRANSFER_RECEIVED,
  BILL_PAID,
  CASH_DEPOSIT,
  CASH_WITHDRAWAL,
  SERVICE_REQUEST_UPDATE,
  SECURITY_ALERT
}
