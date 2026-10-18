import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { serviceRequestsApi } from '../../api/serviceRequests.api.js';
import { accountApi } from '../../api/accounts.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDateTime } from '../../utils/formatDate.js';

export default function CustomerServiceRequestsPage() {
  const { showSuccess, showError } = useToast();

  const [requests, setRequests] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      type: 'GENERAL_SUPPORT',
      subject: '',
      description: '',
      relatedAccountId: '',
    },
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [srRes, accRes] = await Promise.all([
        serviceRequestsApi.getMyRequests({ page: 0, size: 20 }),
        accountApi.getMyAccounts(),
      ]);
      setRequests(srRes.data.data?.content || []);
      setAccounts(accRes.data.data || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load tickets.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleOpenNew = () => {
    reset({
      type: 'GENERAL_SUPPORT',
      subject: '',
      description: '',
      relatedAccountId: '',
    });
    setShowModal(true);
  };

  const onSubmit = async (values) => {
    try {
      setSubmitting(true);
      const payload = {
        type: values.type,
        subject: values.subject.trim(),
        description: values.description.trim(),
        relatedAccountId: values.relatedAccountId ? Number(values.relatedAccountId) : null,
      };
      await serviceRequestsApi.createRequest(payload);
      showSuccess('Support ticket submitted successfully.');
      setShowModal(false);
      fetchData();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to submit ticket.');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'SUBMITTED':
        return <Badge bg="secondary">Submitted</Badge>;
      case 'UNDER_REVIEW':
        return <Badge bg="warning" text="dark">Under Review</Badge>;
      case 'RESOLVED':
        return <Badge bg="success">Resolved</Badge>;
      case 'REJECTED':
        return <Badge bg="danger">Rejected</Badge>;
      default:
        return <Badge bg="light" text="dark">{status}</Badge>;
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">Customer Support Tickets</h2>
          <p className="text-muted mb-0">Open requests for profile corrections, account closures, or general banking inquiries</p>
        </div>
        <Button variant="primary" onClick={handleOpenNew}>
          <i className="bi bi-plus-circle me-1"></i> Open New Ticket
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : requests.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-chat-left-dots fs-1 d-block mb-2"></i>
              No support tickets found. Click "Open New Ticket" if you need assistance.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Ticket #</th>
                    <th>Type</th>
                    <th>Subject</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th className="text-end">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((r) => (
                    <tr key={r.id}>
                      <td><code>#{r.id}</code></td>
                      <td>
                        <Badge bg="info" className="text-dark">
                          {r.type.replace('_', ' ')}
                        </Badge>
                      </td>
                      <td className="fw-semibold">{r.subject}</td>
                      <td>{getStatusBadge(r.status)}</td>
                      <td className="text-muted small">{formatDateTime(r.createdAt)}</td>
                      <td className="text-end">
                        <Button
                          variant="outline-secondary"
                          size="sm"
                          onClick={() => setSelectedRequest(r)}
                        >
                          View Details
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>
      </Card>

      {/* New Ticket Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">Open Support Ticket</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit(onSubmit)}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Request Type *</Form.Label>
              <Form.Select {...register('type', { required: true })}>
                <option value="GENERAL_SUPPORT">General Support / Inquiries</option>
                <option value="PROFILE_CORRECTION">Profile / Details Correction</option>
                <option value="ACCOUNT_CLOSURE">Account Closure Request</option>
              </Form.Select>
            </Form.Group>

            {accounts.length > 0 && (
              <Form.Group className="mb-3">
                <Form.Label className="fw-semibold">Related Account (Optional)</Form.Label>
                <Form.Select {...register('relatedAccountId')}>
                  <option value="">None / Not Account Specific</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountNumber || 'Pending Account'} ({a.type})
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            )}

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Subject *</Form.Label>
              <Form.Control
                type="text"
                placeholder="Brief summary of your request"
                {...register('subject', { required: 'Subject is required', maxLength: 200 })}
                isInvalid={!!errors.subject}
              />
              <Form.Control.Feedback type="invalid">
                {errors.subject?.message}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Description *</Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                placeholder="Detailed explanation of the issue or request"
                {...register('description', { required: 'Description is required', maxLength: 2000 })}
                isInvalid={!!errors.description}
              />
              <Form.Control.Feedback type="invalid">
                {errors.description?.message}
              </Form.Control.Feedback>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? <Spinner size="sm" className="me-2" /> : null}
              Submit Ticket
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Ticket Details Modal */}
      <Modal show={!!selectedRequest} onHide={() => setSelectedRequest(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">
            Ticket #{selectedRequest?.id}: {selectedRequest?.subject}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedRequest && (
            <div>
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <Badge bg="info" className="text-dark me-2">
                    {selectedRequest.type.replace('_', ' ')}
                  </Badge>
                  {getStatusBadge(selectedRequest.status)}
                </div>
                <div className="small text-muted">{formatDateTime(selectedRequest.createdAt)}</div>
              </div>

              <div className="mb-3">
                <label className="fw-semibold text-muted small">Description:</label>
                <div className="p-3 bg-light rounded">{selectedRequest.description}</div>
              </div>

              {selectedRequest.resolutionNotes && (
                <div className="mb-3">
                  <label className="fw-semibold text-muted small">Staff Resolution / Notes:</label>
                  <div className="p-3 bg-white border rounded border-success">
                    {selectedRequest.resolutionNotes}
                  </div>
                  {selectedRequest.resolvedAt && (
                    <div className="small text-muted mt-1">
                      Resolved on: {formatDateTime(selectedRequest.resolvedAt)}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setSelectedRequest(null)}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
