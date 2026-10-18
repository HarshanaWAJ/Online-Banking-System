import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { serviceRequestsApi } from '../../api/serviceRequests.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDateTime } from '../../utils/formatDate.js';

export default function StaffServiceRequestsPage() {
  const { showSuccess, showError } = useToast();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Resolution Form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      status: 'RESOLVED',
      resolutionNotes: '',
    },
  });

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const params = { page: 0, size: 50 };
      if (statusFilter) params.status = statusFilter;
      const res = await serviceRequestsApi.searchRequests(params);
      setRequests(res.data.data?.content || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load support requests queue.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [statusFilter]);

  const handleAssignToMe = async (id) => {
    try {
      await serviceRequestsApi.assignToMe(id);
      showSuccess('Ticket assigned to you.');
      fetchRequests();
      if (selectedRequest && selectedRequest.id === id) {
        setSelectedRequest(null);
      }
    } catch (err) {
      showError(err.apiError?.message || 'Failed to assign ticket.');
    }
  };

  const handleOpenResolve = (r) => {
    setSelectedRequest(r);
    reset({
      status: 'RESOLVED',
      resolutionNotes: '',
    });
  };

  const onSubmitResolution = async (values) => {
    try {
      setSubmitting(true);
      await serviceRequestsApi.resolveRequest(selectedRequest.id, {
        status: values.status,
        resolutionNotes: values.resolutionNotes.trim(),
      });
      showSuccess(`Ticket #${selectedRequest.id} marked as ${values.status}.`);
      setSelectedRequest(null);
      fetchRequests();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to resolve ticket.');
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
          <h2 className="fw-bold mb-1">Customer Support Desk</h2>
          <p className="text-muted mb-0">Review, assign, and resolve customer support inquiries and service tickets</p>
        </div>
        <div className="d-flex gap-2">
          <Form.Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ width: '180px' }}
          >
            <option value="">All Statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="RESOLVED">Resolved</option>
            <option value="REJECTED">Rejected</option>
          </Form.Select>
          <Button variant="outline-secondary" onClick={fetchRequests}>
            <i className="bi bi-arrow-clockwise"></i>
          </Button>
        </div>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : requests.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-inbox fs-1 d-block mb-2"></i>
              No tickets found in queue.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>ID</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Subject</th>
                    <th>Assigned To</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((r) => (
                    <tr key={r.id}>
                      <td><code>#{r.id}</code></td>
                      <td>
                        <div className="fw-semibold">{r.customerName}</div>
                        <div className="text-muted small">{r.customerEmail}</div>
                      </td>
                      <td>
                        <Badge bg="info" className="text-dark">
                          {r.type.replace('_', ' ')}
                        </Badge>
                      </td>
                      <td>
                        <div className="fw-semibold">{r.subject}</div>
                        <div className="text-muted small">{formatDateTime(r.createdAt)}</div>
                      </td>
                      <td>
                        {r.assignedStaffEmail ? (
                          <span className="small text-muted">{r.assignedStaffEmail}</span>
                        ) : (
                          <span className="badge bg-light text-dark border">Unassigned</span>
                        )}
                      </td>
                      <td>{getStatusBadge(r.status)}</td>
                      <td className="text-end">
                        {!r.assignedStaffEmail && r.status !== 'RESOLVED' && r.status !== 'REJECTED' && (
                          <Button
                            variant="outline-primary"
                            size="sm"
                            className="me-2"
                            onClick={() => handleAssignToMe(r.id)}
                          >
                            Assign to Me
                          </Button>
                        )}
                        {r.status !== 'RESOLVED' && r.status !== 'REJECTED' ? (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleOpenResolve(r)}
                          >
                            Resolve
                          </Button>
                        ) : (
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            onClick={() => handleOpenResolve(r)}
                          >
                            View
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>
      </Card>

      {/* Resolve / View Modal */}
      <Modal show={!!selectedRequest} onHide={() => setSelectedRequest(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">
            Ticket #{selectedRequest?.id}: {selectedRequest?.subject}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit(onSubmitResolution)}>
          <Modal.Body>
            {selectedRequest && (
              <div>
                <div className="p-3 bg-light rounded mb-3">
                  <div><strong>Customer:</strong> {selectedRequest.customerName} ({selectedRequest.customerEmail})</div>
                  <div><strong>Type:</strong> {selectedRequest.type.replace('_', ' ')}</div>
                  {selectedRequest.relatedAccountId && (
                    <div><strong>Related Account ID:</strong> {selectedRequest.relatedAccountId}</div>
                  )}
                  <div className="mt-2"><strong>Details:</strong></div>
                  <p className="mb-0 text-muted">{selectedRequest.description}</p>
                </div>

                {selectedRequest.status === 'RESOLVED' || selectedRequest.status === 'REJECTED' ? (
                  <div>
                    <div className="fw-semibold mb-1">Status: {getStatusBadge(selectedRequest.status)}</div>
                    <div className="fw-semibold text-muted small">Resolution Notes:</div>
                    <div className="p-2 border rounded bg-white">{selectedRequest.resolutionNotes || '—'}</div>
                  </div>
                ) : (
                  <div>
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">Decision *</Form.Label>
                      <Form.Select {...register('status', { required: true })}>
                        <option value="RESOLVED">Resolve (Accept / Completed)</option>
                        <option value="REJECTED">Reject Request</option>
                      </Form.Select>
                    </Form.Group>

                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">Staff Notes / Response to Customer *</Form.Label>
                      <Form.Control
                        as="textarea"
                        rows={3}
                        placeholder="Explain resolution steps taken or reason for rejection"
                        {...register('resolutionNotes', { required: 'Resolution notes are required' })}
                        isInvalid={!!errors.resolutionNotes}
                      />
                      <Form.Control.Feedback type="invalid">
                        {errors.resolutionNotes?.message}
                      </Form.Control.Feedback>
                    </Form.Group>
                  </div>
                )}
              </div>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setSelectedRequest(null)} disabled={submitting}>
              Close
            </Button>
            {selectedRequest && selectedRequest.status !== 'RESOLVED' && selectedRequest.status !== 'REJECTED' && (
              <Button variant="success" type="submit" disabled={submitting}>
                {submitting ? <Spinner size="sm" className="me-2" /> : null}
                Save Resolution
              </Button>
            )}
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
