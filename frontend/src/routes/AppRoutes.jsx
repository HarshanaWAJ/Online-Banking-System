import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import AdminLayout from '../layouts/AdminLayout.jsx';
import CustomerLayout from '../layouts/CustomerLayout.jsx';
import PublicLayout from '../layouts/PublicLayout.jsx';
import StaffLayout from '../layouts/StaffLayout.jsx';
import ForbiddenPage from '../pages/ForbiddenPage.jsx';
import NotFoundPage from '../pages/NotFoundPage.jsx';
import ProtectedRoute from './ProtectedRoute.jsx';
import PATHS from './routePaths.js';

// Feature Pages: Authentication & Profile
import LoginPage from '../features/auth/LoginPage.jsx';
import RegisterPage from '../features/auth/RegisterPage.jsx';
import ChangePasswordPage from '../features/auth/ChangePasswordPage.jsx';
import ProfilePage from '../features/customers/ProfilePage.jsx';

// Feature Pages: Accounts & Transactions
import CustomerDashboardPage from '../features/accounts/CustomerDashboardPage.jsx';
import RequestAccountPage from '../features/accounts/RequestAccountPage.jsx';
import StaffAccountRequestsPage from '../features/accounts/StaffAccountRequestsPage.jsx';
import BeneficiaryListPage from '../features/beneficiaries/BeneficiaryListPage.jsx';
import TransferPage from '../features/transfers/TransferPage.jsx';
import TransactionHistoryPage from '../features/transactions/TransactionHistoryPage.jsx';
import StaffCashOperationPage from '../features/cash-operations/StaffCashOperationPage.jsx';

// Feature Pages: Bill Payments & Service Requests
import PayBillPage from '../features/bill-payments/PayBillPage.jsx';
import BillPaymentHistoryPage from '../features/bill-payments/BillPaymentHistoryPage.jsx';
import AdminBillerListPage from '../features/admin/AdminBillerListPage.jsx';
import CustomerServiceRequestsPage from '../features/service-requests/CustomerServiceRequestsPage.jsx';
import StaffServiceRequestsPage from '../features/service-requests/StaffServiceRequestsPage.jsx';

// Feature Pages: Staff & Admin Management
import AdminUsersPage from '../features/admin/AdminUsersPage.jsx';
import ReportsDashboardPage from '../features/reports/ReportsDashboardPage.jsx';
import AdminAuditLogPage from '../features/admin/AdminAuditLogPage.jsx';
import AdminSettingsPage from '../features/admin/AdminSettingsPage.jsx';
import NotificationsPage from '../features/notifications/NotificationsPage.jsx';
import StaffCustomerDirectoryPage from '../features/customers/StaffCustomerDirectoryPage.jsx';

/**
 * Root Landing Redirector (`/`).
 *
 * Dynamically resolves the starting screen based on the user's role:
 * - Unauthenticated / Guest -> `/login`
 * - ADMIN -> `/admin/users` (User management console)
 * - STAFF -> `/staff/account-requests` (Pending applications queue)
 * - CUSTOMER -> `/dashboard` (Customer accounts overview)
 */
function RootRedirect() {
  const { user, loading } = useAuth();

  // Defer redirect until AuthContext finishes checking the session cookie
  if (loading) return null;

  // Unauthenticated visitors are sent to the login screen
  if (!user) return <Navigate to={PATHS.LOGIN} replace />;

  // Direct users to their primary workspace depending on their role
  if (user.role === 'ADMIN') return <Navigate to={PATHS.ADMIN_USERS} replace />;
  if (user.role === 'STAFF') return <Navigate to={PATHS.STAFF_ACCOUNT_REQUESTS} replace />;
  return <Navigate to={PATHS.DASHBOARD} replace />;
}

/**
 * Main application routes hierarchy.
 *
 * Architecture:
 * - Uses React Router v6 nested routes pattern.
 * - Outlets within layout components (`PublicLayout`, `CustomerLayout`, `StaffLayout`, `AdminLayout`)
 *   render child components inside consistent navigational chrome (navbars, sidebars, footers).
 * - Routes are wrapped in `<ProtectedRoute>` to guard access based on user role.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* =========================================================================
          1. PUBLIC ROUTES (Unauthenticated Guest Zone)
          ========================================================================= */}
      <Route element={<PublicLayout />}>
        <Route path={PATHS.LOGIN} element={<LoginPage />} />
        <Route path={PATHS.REGISTER} element={<RegisterPage />} />
      </Route>

      {/* =========================================================================
          2. CUSTOMER PORTAL ROUTES (Role: CUSTOMER)
          Requires authenticated user with CUSTOMER role.
          ========================================================================= */}
      <Route
        element={
          <ProtectedRoute allowedRoles={['CUSTOMER']}>
            <CustomerLayout />
          </ProtectedRoute>
        }
      >
        <Route path={PATHS.DASHBOARD} element={<CustomerDashboardPage />} />
        <Route path={PATHS.REQUEST_ACCOUNT} element={<RequestAccountPage />} />
        <Route path={PATHS.ACCOUNT_DETAILS} element={<TransactionHistoryPage />} />
        <Route path={PATHS.TRANSACTIONS} element={<TransactionHistoryPage />} />
        <Route path={PATHS.BENEFICIARIES} element={<BeneficiaryListPage />} />
        <Route path={PATHS.TRANSFER} element={<TransferPage />} />
        <Route path={PATHS.PAY_BILL} element={<PayBillPage />} />
        <Route path={PATHS.BILL_PAYMENT_HISTORY} element={<BillPaymentHistoryPage />} />
        <Route path={PATHS.SERVICE_REQUESTS} element={<CustomerServiceRequestsPage />} />
        <Route path={PATHS.SERVICE_REQUEST_NEW} element={<CustomerServiceRequestsPage />} />
        <Route path={PATHS.SERVICE_REQUEST_DETAIL} element={<CustomerServiceRequestsPage />} />
        <Route path={PATHS.NOTIFICATIONS} element={<NotificationsPage />} />
        <Route path={PATHS.PROFILE} element={<ProfilePage />} />
        <Route path={PATHS.CHANGE_PASSWORD} element={<ChangePasswordPage />} />
      </Route>

      {/* =========================================================================
          3. STAFF OPERATIONAL ROUTES (Role: STAFF)
          Requires authenticated employee with STAFF role.
          ========================================================================= */}
      <Route
        element={
          <ProtectedRoute allowedRoles={['STAFF']}>
            <StaffLayout />
          </ProtectedRoute>
        }
      >
        <Route path={PATHS.STAFF_ACCOUNT_REQUESTS} element={<StaffAccountRequestsPage />} />
        <Route path={PATHS.STAFF_CUSTOMERS} element={<StaffCustomerDirectoryPage />} />
        <Route path={PATHS.STAFF_CUSTOMER_DETAIL} element={<StaffCustomerDirectoryPage />} />
        <Route path={PATHS.STAFF_ACCOUNT_DETAIL} element={<TransactionHistoryPage />} />
        <Route path={PATHS.STAFF_CASH_OPERATIONS} element={<StaffCashOperationPage />} />
        <Route path={PATHS.STAFF_SERVICE_REQUESTS} element={<StaffServiceRequestsPage />} />
        <Route path={PATHS.STAFF_SERVICE_REQUEST_DETAIL} element={<StaffServiceRequestsPage />} />
        <Route path={PATHS.STAFF_REPORTS} element={<ReportsDashboardPage />} />
        <Route path={PATHS.NOTIFICATIONS} element={<NotificationsPage />} />
        <Route path={PATHS.CHANGE_PASSWORD} element={<ChangePasswordPage />} />
      </Route>

      {/* =========================================================================
          4. ADMIN GOVERNANCE ROUTES (Role: ADMIN)
          Requires authenticated administrator with ADMIN role.
          ========================================================================= */}
      <Route
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route path={PATHS.ADMIN_USERS} element={<AdminUsersPage />} />
        <Route path={PATHS.ADMIN_BILLERS} element={<AdminBillerListPage />} />
        <Route path={PATHS.ADMIN_REPORTS} element={<ReportsDashboardPage />} />
        <Route path={PATHS.ADMIN_AUDIT} element={<AdminAuditLogPage />} />
        <Route path={PATHS.ADMIN_SETTINGS} element={<AdminSettingsPage />} />
        <Route path={PATHS.NOTIFICATIONS} element={<NotificationsPage />} />
        <Route path={PATHS.CHANGE_PASSWORD} element={<ChangePasswordPage />} />
      </Route>

      {/* =========================================================================
          5. ROOT ENTRY POINT & SYSTEM ERROR STATUS PAGES
          ========================================================================= */}
      {/* Root path '/' delegates to smart role-based redirect */}
      <Route path="/" element={<RootRedirect />} />

      {/* 403 Forbidden Access Denied Page */}
      <Route path={PATHS.FORBIDDEN} element={<ForbiddenPage />} />

      {/* 404 Catch-All Page for unmatched paths */}
      <Route path={PATHS.NOT_FOUND} element={<NotFoundPage />} />
    </Routes>
  );
}
