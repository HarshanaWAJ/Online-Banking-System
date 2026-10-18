package com.bankingsystem.constants;

/**
 * Centralized API route constants.
 * All controller route mappings must reference these constants.
 */
public final class ApiPaths {

  public static final String API = "/api";

  // Auth
  public static final String AUTH = API + "/auth";
  public static final String AUTH_REGISTER = AUTH + "/register";
  public static final String AUTH_LOGIN = AUTH + "/login";
  public static final String AUTH_LOGOUT = AUTH + "/logout";
  public static final String AUTH_ME = AUTH + "/me";
  public static final String AUTH_CHANGE_PASSWORD = AUTH + "/change-password";

  // Customers
  public static final String CUSTOMERS = API + "/customers";

  // Accounts
  public static final String ACCOUNTS = API + "/accounts";

  // Beneficiaries
  public static final String BENEFICIARIES = API + "/beneficiaries";

  // Transfers
  public static final String TRANSFERS = API + "/transfers";

  // Billers
  public static final String BILLERS = API + "/billers";

  // Bill Payments
  public static final String BILL_PAYMENTS = API + "/bill-payments";

  // Cash Operations
  public static final String CASH_OPERATIONS = API + "/cash-operations";
  public static final String COMPENSATIONS = API + "/compensations";

  // Service Requests
  public static final String SERVICE_REQUESTS = API + "/service-requests";

  // Admin Users
  public static final String ADMIN_USERS = API + "/admin/users";

  // Reports & Audit
  public static final String REPORTS = API + "/reports";
  public static final String AUDIT_EVENTS = API + "/audit-events";

  // Notifications
  public static final String NOTIFICATIONS = API + "/notifications";

  // Admin Settings
  public static final String ADMIN_SETTINGS = API + "/admin/settings";

  private ApiPaths() {
    // Utility class — prevent instantiation
  }
}
