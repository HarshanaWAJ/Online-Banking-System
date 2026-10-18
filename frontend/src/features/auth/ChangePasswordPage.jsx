import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { authApi } from '../../api/auth.api.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function ChangePasswordPage() {
  const { showSuccess, showError } = useToast();

  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm();

  const newPasswordVal = watch('newPassword', '');

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await authApi.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      showSuccess('Password updated successfully. Other active sessions were invalidated.');
      reset();
    } catch (err) {
      const msg = err.apiError?.message || 'Failed to update password.';
      setServerError(msg);
      showError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="shadow-sm mx-auto" style={{ maxWidth: '480px' }}>
      <Card.Header className="bg-white py-3">
        <h5 className="mb-0 fw-semibold">Change Password</h5>
      </Card.Header>
      <Card.Body className="p-4">
        {serverError && (
          <Alert variant="danger" className="py-2 small">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            {serverError}
          </Alert>
        )}

        <Form onSubmit={handleSubmit(onSubmit)}>
          <Form.Group className="mb-3" controlId="current-password">
            <Form.Label>Current Password</Form.Label>
            <Form.Control
              type="password"
              isInvalid={!!errors.currentPassword}
              {...register('currentPassword', { required: 'Current password is required' })}
            />
            <Form.Control.Feedback type="invalid">{errors.currentPassword?.message}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3" controlId="new-password">
            <Form.Label>New Password</Form.Label>
            <Form.Control
              type="password"
              isInvalid={!!errors.newPassword}
              {...register('newPassword', {
                required: 'New password is required',
                minLength: { value: 8, message: 'Password must be at least 8 characters' },
                pattern: {
                  value: /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$/,
                  message: 'Must include uppercase, lowercase, and numeric character',
                },
              })}
            />
            <Form.Control.Feedback type="invalid">{errors.newPassword?.message}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-4" controlId="confirm-new-password">
            <Form.Label>Confirm New Password</Form.Label>
            <Form.Control
              type="password"
              isInvalid={!!errors.confirmNewPassword}
              {...register('confirmNewPassword', {
                required: 'Please confirm password',
                validate: (val) => val === newPasswordVal || 'Passwords do not match',
              })}
            />
            <Form.Control.Feedback type="invalid">{errors.confirmNewPassword?.message}</Form.Control.Feedback>
          </Form.Group>

          <Button variant="primary" type="submit" disabled={submitting} className="w-100">
            {submitting ? (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                Updating Password...
              </>
            ) : (
              'Update Password'
            )}
          </Button>
        </Form>
      </Card.Body>
    </Card>
  );
}
