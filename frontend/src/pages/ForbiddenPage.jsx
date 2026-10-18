import { Button, Container } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import PATHS from '../routes/routePaths.js';

/**
 * 403 Forbidden Page with role-aware return destination.
 */
export default function ForbiddenPage() {
  const { user } = useAuth();

  const getHomePath = () => {
    if (!user) return PATHS.LOGIN;
    if (user.role === 'ADMIN') return PATHS.ADMIN_USERS;
    if (user.role === 'STAFF') return PATHS.STAFF_ACCOUNT_REQUESTS;
    return PATHS.DASHBOARD;
  };

  return (
    <Container className="text-center py-5">
      <i className="bi bi-shield-slash text-danger display-1 d-block mb-3" />
      <h1 className="display-5 fw-bold mb-3">403 — Access Denied</h1>
      <p className="text-secondary lead mb-4">
        You do not have permission to view or access this resource.
      </p>
      <Button as={Link} to={getHomePath()} variant="primary">
        <i className="bi bi-arrow-left me-2" />
        Return to Home
      </Button>
    </Container>
  );
}
