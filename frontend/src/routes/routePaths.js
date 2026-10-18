/**
 * Centralized route path constants.
 * Use these everywhere — never hardcode path strings in components.
 */
const PATHS = {
  // Public
  LOGIN: '/login',
  REGISTER: '/register',

  // Customer
  DASHBOARD: '/dashboard',
  ACCOUNT_DETAILS: '/accounts/:id',
  REQUEST_ACCOUNT: '/accounts/request',
  BENEFICIARIES: '/beneficiaries',
  TRANSFER: '/transfer',
  PAY_BILL: '/pay-bill',
  BILL_PAYMENT_HISTORY: '/bill-payments',
  TRANSACTIONS: '/accounts/:id/transactions',
  SERVICE_REQUESTS: '/service-requests',
  SERVICE_REQUEST_DETAIL: '/service-requests/:id',
  SERVICE_REQUEST_NEW: '/service-requests/new',
  NOTIFICATIONS: '/notifications',
  PROFILE: '/profile',
  CHANGE_PASSWORD: '/change-password',

  // Staff
  STAFF_ACCOUNT_REQUESTS: '/staff/account-requests',
  STAFF_CUSTOMERS: '/staff/customers',
  STAFF_CUSTOMER_DETAIL: '/staff/customers/:id',
  STAFF_ACCOUNT_DETAIL: '/staff/accounts/:id',
  STAFF_CASH_OPERATIONS: '/staff/cash-operations',
  STAFF_SERVICE_REQUESTS: '/staff/service-requests',
  STAFF_SERVICE_REQUEST_DETAIL: '/staff/service-requests/:id',
  STAFF_REPORTS: '/staff/reports',

  // Admin
  ADMIN_USERS: '/admin/users',
  ADMIN_BILLERS: '/admin/billers',
  ADMIN_REPORTS: '/admin/reports',
  ADMIN_AUDIT: '/admin/audit',
  ADMIN_SETTINGS: '/admin/settings',

  // Utility
  NOT_FOUND: '*',
  FORBIDDEN: '/forbidden',
};

export default PATHS;
