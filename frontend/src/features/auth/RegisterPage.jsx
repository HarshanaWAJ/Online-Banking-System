import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Col, Form, Row, Spinner } from 'react-bootstrap';
import { authApi } from '../../api/auth.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();

  const passwordValue = watch('password', '');

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await authApi.register({
        email: data.email,
        password: data.password,
        fullName: data.fullName,
        nic: data.nic,
        phone: data.phone,
        address: data.address,
        dateOfBirth: data.dateOfBirth,
      });

      showSuccess('Registration successful! Please sign in with your credentials.');
      navigate(PATHS.LOGIN, { replace: true });
    } catch (err) {
      const message = err.apiError?.message || 'Registration failed. Please check your details.';
      setServerError(message);
      showError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="shadow-sm mx-auto" style={{ maxWidth: '640px', width: '100%' }}>
      <Card.Body className="p-4">
        <div className="text-center mb-4">
          <i className="bi bi-person-plus-fill fs-1 text-primary d-block mb-2" />
          <h2 className="h4 fw-bold mb-1">Create Customer Account</h2>
          <p className="text-muted small">Register to experience online banking services</p>
        </div>

        {serverError && (
          <Alert variant="danger" className="py-2 small">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            {serverError}
          </Alert>
        )}

        <Form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Row>
            <Col md={12} className="mb-3">
              <Form.Group controlId="register-fullName">
                <Form.Label>Full Name</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="e.g. Sunil Perera"
                  isInvalid={!!errors.fullName}
                  {...register('fullName', { required: 'Full name is required' })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.fullName?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-3">
              <Form.Group controlId="register-nic">
                <Form.Label>National Identity Card (NIC)</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="e.g. 199012345678"
                  isInvalid={!!errors.nic}
                  {...register('nic', {
                    required: 'NIC number is required',
                    minLength: { value: 9, message: 'NIC must be at least 9 characters' },
                  })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.nic?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-3">
              <Form.Group controlId="register-dob">
                <Form.Label>Date of Birth</Form.Label>
                <Form.Control
                  type="date"
                  isInvalid={!!errors.dateOfBirth}
                  {...register('dateOfBirth', { required: 'Date of birth is required' })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.dateOfBirth?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-3">
              <Form.Group controlId="register-phone">
                <Form.Label>Mobile Phone</Form.Label>
                <Form.Control
                  type="tel"
                  placeholder="+94 77 123 4567"
                  isInvalid={!!errors.phone}
                  {...register('phone', { required: 'Phone number is required' })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.phone?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-3">
              <Form.Group controlId="register-email">
                <Form.Label>Email Address</Form.Label>
                <Form.Control
                  type="email"
                  placeholder="name@example.com"
                  isInvalid={!!errors.email}
                  {...register('email', {
                    required: 'Email address is required',
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
            </Col>

            <Col md={12} className="mb-3">
              <Form.Group controlId="register-address">
                <Form.Label>Postal Address</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={2}
                  placeholder="Street address, city"
                  isInvalid={!!errors.address}
                  {...register('address', { required: 'Address is required' })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.address?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-3">
              <Form.Group controlId="register-password">
                <Form.Label>Password</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="Min. 8 chars (Upper, lower, digit)"
                  isInvalid={!!errors.password}
                  {...register('password', {
                    required: 'Password is required',
                    minLength: { value: 8, message: 'Password must be at least 8 characters' },
                    pattern: {
                      value: /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$/,
                      message: 'Include at least one uppercase, one lowercase, and one digit',
                    },
                  })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.password?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>

            <Col md={6} className="mb-4">
              <Form.Group controlId="register-confirmPassword">
                <Form.Label>Confirm Password</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="Re-enter password"
                  isInvalid={!!errors.confirmPassword}
                  {...register('confirmPassword', {
                    required: 'Please confirm password',
                    validate: (val) => val === passwordValue || 'Passwords do not match',
                  })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.confirmPassword?.message}
                </Form.Control.Feedback>
              </Form.Group>
            </Col>
          </Row>

          <Button variant="primary" type="submit" className="w-100 py-2 fw-semibold" disabled={submitting}>
            {submitting ? (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                Submitting registration...
              </>
            ) : (
              'Complete Registration'
            )}
          </Button>
        </Form>

        <div className="text-center mt-4 pt-3 border-top">
          <span className="text-muted small">Already have an account? </span>
          <Link to={PATHS.LOGIN} className="small fw-semibold text-decoration-none">
            Sign In here
          </Link>
        </div>
      </Card.Body>
    </Card>
  );
}
