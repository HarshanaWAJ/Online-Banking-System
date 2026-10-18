import { Alert } from 'react-bootstrap';

/**
 * Standard error alert component.
 */
export default function ErrorAlert({ message = 'An error occurred. Please try again.' }) {
  return (
    <Alert variant="danger" className="d-flex align-items-center">
      <i className="bi bi-exclamation-circle-fill me-2 flex-shrink-0" />
      <div>{message}</div>
    </Alert>
  );
}
