import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import PATHS from './routePaths.js';

/**
 * Wraps a route requiring authentication and optional role restriction.
 * Unauthenticated users are redirected to /login with the attempted path saved.
 * Users with the wrong role see the /forbidden page.
 */
export default function ProtectedRoute({ children, allowedRoles }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to={PATHS.LOGIN} state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={PATHS.FORBIDDEN} replace />;
  }

  return children;
}
