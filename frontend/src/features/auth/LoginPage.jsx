import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Form, Spinner } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';

export default function LoginPage() {
  const { login } = useAuth();
  const { showError } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      const user = await login(data);
      if (user.role === 'STAFF') {
        navigate(PATHS.STAFF_ACCOUNT_REQUESTS, { replace: true });
      } else if (user.role === 'ADMIN') {
        navigate(PATHS.ADMIN_USERS, { replace: true });
      } else {
        const from = location.state?.from?.pathname || PATHS.DASHBOARD;
        navigate(from, { replace: true });
      }
    } catch (err) {
      const message = err.apiError?.message || 'Invalid email or password.';
      setServerError(message);
      showError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="shadow-sm mx-auto" style={{ maxWidth: '440px', width: '100%' }}>
      <Card.Body className="p-4">
        <div className="text-center mb-4">
          <i className="bi bi-bank2 fs-1 text-primary d-block mb-2" />
          <h2 className="h4 fw-bold mb-1">Sign In to Online Banking</h2>
          <p className="text-muted small">Enter your demo credentials to access your portal</p>
        </div>

        {serverError && (
          <Alert variant="danger" className="py-2 small">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            {serverError}
          </Alert>
        )}

        <Form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Form.Group className="mb-3" controlId="login-email">
            <Form.Label>Email Address</Form.Label>
            <Form.Control
              type="email"
              placeholder="name@bankdemo.lk"
              autoComplete="username"
              isInvalid={!!errors.email}
              {...register('email', {
                required: 'Email is required',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Enter a valid email address',
                },
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.email?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-4" controlId="login-password">
            <Form.Label>Password</Form.Label>
            <Form.Control
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              isInvalid={!!errors.password}
              {...register('password', {
                required: 'Password is required',
              })}
            />
            <Form.Control.Feedback type="invalid">
              {errors.password?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Button variant="primary" type="submit" className="w-100 py-2 fw-semibold" disabled={submitting}>
            {submitting ? (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                Signing in...
              </>
            ) : (
              'Sign In'
            )}
          </Button>
        </Form>

        <div className="text-center mt-4 pt-3 border-top">
          <span className="text-muted small">Do not have an account? </span>
          <Link to={PATHS.REGISTER} className="small fw-semibold text-decoration-none">
            Register here
          </Link>
        </div>
      </Card.Body>
    </Card>
  );
}
