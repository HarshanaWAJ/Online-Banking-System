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

// Feature Pages
import LoginPage from '../features/auth/LoginPage.jsx';
import RegisterPage from '../features/auth/RegisterPage.jsx';
import ChangePasswordPage from '../features/auth/ChangePasswordPage.jsx';
import ProfilePage from '../features/customers/ProfilePage.jsx';
import CustomerDashboardPage from '../features/accounts/CustomerDashboardPage.jsx';
import RequestAccountPage from '../features/accounts/RequestAccountPage.jsx';
import StaffAccountRequestsPage from '../features/accounts/StaffAccountRequestsPage.jsx';
import BeneficiaryListPage from '../features/beneficiaries/BeneficiaryListPage.jsx';
import TransferPage from '../features/transfers/TransferPage.jsx';
import TransactionHistoryPage from '../features/transactions/TransactionHistoryPage.jsx';
import StaffCashOperationPage from '../features/cash-operations/StaffCashOperationPage.jsx';
import PayBillPage from '../features/bill-payments/PayBillPage.jsx';
import BillPaymentHistoryPage from '../features/bill-payments/BillPaymentHistoryPage.jsx';
import AdminBillerListPage from '../features/admin/AdminBillerListPage.jsx';
import CustomerServiceRequestsPage from '../features/service-requests/CustomerServiceRequestsPage.jsx';
import StaffServiceRequestsPage from '../features/service-requests/StaffServiceRequestsPage.jsx';
import AdminUsersPage from '../features/admin/AdminUsersPage.jsx';
import ReportsDashboardPage from '../features/reports/ReportsDashboardPage.jsx';
import AdminAuditLogPage from '../features/admin/AdminAuditLogPage.jsx';
import AdminSettingsPage from '../features/admin/AdminSettingsPage.jsx';
import NotificationsPage from '../features/notifications/NotificationsPage.jsx';
import StaffCustomerDirectoryPage from '../features/customers/StaffCustomerDirectoryPage.jsx';

/**
 * Directs authenticated users to their role landing dashboard, or guests to login.
 */
function RootRedirect() {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to={PATHS.LOGIN} replace />;
  if (user.role === 'ADMIN') return <Navigate to={PATHS.ADMIN_USERS} replace />;
  if (user.role === 'STAFF') return <Navigate to={PATHS.STAFF_ACCOUNT_REQUESTS} replace />;
  return <Navigate to={PATHS.DASHBOARD} replace />;
}

/**
 * Main application routes hierarchy.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route element={<PublicLayout />}>
        <Route path={PATHS.LOGIN} element={<LoginPage />} />
        <Route path={PATHS.REGISTER} element={<RegisterPage />} />
      </Route>

      {/* Customer routes */}
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

      {/* Staff routes */}
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

      {/* Admin routes */}
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

      {/* Default root redirect */}
      <Route path="/" element={<RootRedirect />} />

      {/* Status pages */}
      <Route path={PATHS.FORBIDDEN} element={<ForbiddenPage />} />
      <Route path={PATHS.NOT_FOUND} element={<NotFoundPage />} />
    </Routes>
  );
}
