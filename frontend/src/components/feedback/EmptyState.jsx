import { Container } from 'react-bootstrap';

/**
 * Standard empty state component with Bootstrap styling.
 */
export default function EmptyState({ icon = 'bi-inbox', title = 'Nothing here yet', message }) {
  return (
    <Container className="text-center py-5 text-muted">
      <i className={`bi ${icon} display-4 d-block mb-3`} />
      <h5 className="mt-2">{title}</h5>
      {message && <p className="mb-0 text-secondary">{message}</p>}
    </Container>
  );
}
