import { Button, Container } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import PATHS from '../routes/routePaths.js';

/**
 * 404 Not Found Page.
 */
export default function NotFoundPage() {
  return (
    <Container className="text-center py-5">
      <i className="bi bi-exclamation-circle text-muted display-1 d-block mb-3" />
      <h1 className="display-5 fw-bold mb-3">404 — Page Not Found</h1>
      <p className="text-secondary lead mb-4">
        The page you are looking for does not exist or may have been moved.
      </p>
      <Button as={Link} to={PATHS.DASHBOARD} variant="primary">
        <i className="bi bi-house-door me-2" />
        Return to Dashboard
      </Button>
    </Container>
  );
}
