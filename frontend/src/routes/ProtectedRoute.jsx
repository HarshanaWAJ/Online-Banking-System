import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import PATHS from './routePaths.js';

/**
 * Route Guard Component (Client-Side Access Control).
 *
 * Enforces two levels of protection:
 * 1. Authentication Check:
 *    Ensures that the visitor has an active authenticated session.
 *    If unauthenticated, redirects to `/login` while saving the current URL in `state.from`
 *    so the user can be bounced back to their intended destination upon successful login.
 *
 * 2. Role-Based Access Control (RBAC):
 *    If `allowedRoles` array is specified (e.g., ['ADMIN'] or ['STAFF', 'ADMIN']),
 *    verifies that the user's role exists within the authorized list.
 *    If the user lacks the necessary privileges, redirects to `/forbidden` (HTTP 403 equivalent in SPA).
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - The child layout or page component to render if authorized.
 * @param {Array<string>} [props.allowedRoles] - Optional list of authorized roles ('CUSTOMER', 'STAFF', 'ADMIN').
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  const { user } = useAuth();
  const location = useLocation();

  // 1. Guard against unauthenticated access
  if (!user) {
    return <Navigate to={PATHS.LOGIN} state={{ from: location }} replace />;
  }

  // 2. Guard against unauthorized role access
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={PATHS.FORBIDDEN} replace />;
  }

  // Access granted: render the protected content/layout
  return children;
}

