import { Alert, Container } from 'react-bootstrap';

/**
 * Reusable placeholder component for routes under development.
 */
export default function PlaceholderPage({ title, description = 'This feature is scheduled for implementation in subsequent phases.' }) {
  return (
    <Container className="py-4">
      <h1 className="mb-3">{title}</h1>
      <Alert variant="info">
        <i className="bi bi-info-circle-fill me-2" />
        <strong>Notice:</strong> {description}
      </Alert>
    </Container>
  );
}
