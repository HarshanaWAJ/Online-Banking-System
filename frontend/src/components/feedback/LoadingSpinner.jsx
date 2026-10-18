import { Spinner } from 'react-bootstrap';

/**
 * Centered loading spinner.
 */
export default function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="d-flex justify-content-center align-items-center py-5">
      <Spinner animation="border" role="status" variant="primary">
        <span className="visually-hidden">{message}</span>
      </Spinner>
    </div>
  );
}
