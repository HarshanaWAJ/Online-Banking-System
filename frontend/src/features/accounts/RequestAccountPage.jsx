import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { accountApi } from '../../api/accounts.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';

export default function RequestAccountPage() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      type: 'SAVINGS',
    },
  });

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await accountApi.requestAccount({ type: data.type });
      showSuccess('Account request submitted successfully! A bank officer will review it.');
      navigate(PATHS.DASHBOARD, { replace: true });
    } catch (err) {
      const message = err.apiError?.message || 'Failed to submit account request.';
      setServerError(message);
      showError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="shadow-sm mx-auto" style={{ maxWidth: '520px' }}>
      <Card.Header className="bg-white py-3">
        <h5 className="mb-0 fw-semibold">Request a New Bank Account</h5>
      </Card.Header>
      <Card.Body className="p-4">
        {serverError && (
          <Alert variant="danger" className="py-2 small">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            {serverError}
          </Alert>
        )}

        <p className="text-muted small mb-4">
          Choose the type of account you wish to open. New accounts start in a{' '}
          <strong>PENDING</strong> state until approved by a bank officer, at which point a unique 12-digit
          account number will be issued.
        </p>

        <Form onSubmit={handleSubmit(onSubmit)}>
          <Form.Group className="mb-4" controlId="account-type-select">
            <Form.Label className="fw-semibold">Account Type</Form.Label>
            <Form.Select isInvalid={!!errors.type} {...register('type', { required: 'Please select an account type' })}>
              <option value="SAVINGS">Savings Account (Standard savings deposit)</option>
              <option value="CURRENT">Current Account (Checking and frequent transactions)</option>
            </Form.Select>
            <Form.Control.Feedback type="invalid">{errors.type?.message}</Form.Control.Feedback>
          </Form.Group>

          <div className="d-flex justify-content-end gap-2">
            <Button as={Link} to={PATHS.DASHBOARD} variant="outline-secondary">
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? (
                <>
                  <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                  Submitting...
                </>
              ) : (
                'Submit Request'
              )}
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
