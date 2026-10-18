import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Button, Card, Col, Form, Row, Spinner } from 'react-bootstrap';
import { customerApi } from '../../api/customers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDate } from '../../utils/formatDate.js';

export default function ProfilePage() {
  const { showSuccess, showError } = useToast();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const res = await customerApi.getMyProfile();
        setProfile(res.data.data);
        reset({
          phone: res.data.data.phone,
          address: res.data.data.address,
        });
      } catch (err) {
        showError(err.apiError?.message || 'Failed to load profile.');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [reset]);

  const onSubmit = async (data) => {
    setServerError('');
    setUpdating(true);
    try {
      const res = await customerApi.updateMyProfile(data);
      setProfile(res.data.data);
      reset({
        phone: res.data.data.phone,
        address: res.data.data.address,
      });
      showSuccess('Profile updated successfully.');
    } catch (err) {
      const msg = err.apiError?.message || 'Failed to update profile.';
      setServerError(msg);
      showError(msg);
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" role="status" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '720px' }} className="mx-auto">
      <div className="mb-4 pb-2 border-bottom">
        <h1 className="h3 mb-1">Customer Profile</h1>
        <p className="text-muted mb-0 small">View your personal registration details and update contact information</p>
      </div>

      <Card className="shadow-sm border-0 mb-4">
        <Card.Header className="bg-white py-3">
          <h5 className="mb-0 fw-semibold">Personal Information</h5>
        </Card.Header>
        <Card.Body className="p-4">
          <Row className="mb-3">
            <Col sm={4} className="text-muted">Full Name</Col>
            <Col sm={8} className="fw-semibold">{profile?.fullName}</Col>
          </Row>
          <Row className="mb-3">
            <Col sm={4} className="text-muted">NIC Number</Col>
            <Col sm={8} className="font-monospace">{profile?.nic}</Col>
          </Row>
          <Row className="mb-3">
            <Col sm={4} className="text-muted">Date of Birth</Col>
            <Col sm={8}>{formatDate(profile?.dateOfBirth)}</Col>
          </Row>
          <Row className="mb-0">
            <Col sm={4} className="text-muted">Registered Email</Col>
            <Col sm={8}>{profile?.email}</Col>
          </Row>
        </Card.Body>
      </Card>

      <Card className="shadow-sm border-0">
        <Card.Header className="bg-white py-3">
          <h5 className="mb-0 fw-semibold">Contact Details</h5>
        </Card.Header>
        <Card.Body className="p-4">
          {serverError && (
            <Alert variant="danger" className="py-2 small">
              <i className="bi bi-exclamation-triangle-fill me-2" />
              {serverError}
            </Alert>
          )}

          <Form onSubmit={handleSubmit(onSubmit)}>
            <Form.Group className="mb-3" controlId="profile-phone">
              <Form.Label>Mobile Phone</Form.Label>
              <Form.Control
                type="tel"
                isInvalid={!!errors.phone}
                {...register('phone', { required: 'Phone number is required' })}
              />
              <Form.Control.Feedback type="invalid">{errors.phone?.message}</Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-4" controlId="profile-address">
              <Form.Label>Postal Address</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                isInvalid={!!errors.address}
                {...register('address', { required: 'Address is required' })}
              />
              <Form.Control.Feedback type="invalid">{errors.address?.message}</Form.Control.Feedback>
            </Form.Group>

            <Button variant="primary" type="submit" disabled={updating || !isDirty}>
              {updating ? (
                <>
                  <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                  Saving...
                </>
              ) : (
                'Save Changes'
              )}
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}
