import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Form, Modal, Spinner, Table } from 'react-bootstrap';
import { accountApi } from '../../api/accounts.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDate } from '../../utils/formatDate.js';

export default function StaffAccountRequestsPage() {
  const { showSuccess, showError } = useToast();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  // Reject Modal state
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const res = await accountApi.searchAccountRequests({ status: 'PENDING' });
      setRequests(res.data.data || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load account requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleApprove = async (id) => {
    if (!window.confirm('Approve this account request and issue a 12-digit account number?')) {
      return;
    }
    try {
      setActionLoading(true);
      const res = await accountApi.approveAccountRequest(id);
      showSuccess(`Account approved! Assigned Account Number: ${res.data.data.accountNumber}`);
      fetchRequests();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to approve account request.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async (e) => {
    e.preventDefault();
    if (!rejectReason.trim()) {
      showError('Please provide a reason for rejection.');
      return;
    }
    try {
      setActionLoading(true);
      await accountApi.rejectAccountRequest(selectedRequest.id, rejectReason.trim());
      showSuccess('Account request rejected.');
      setShowRejectModal(false);
      setRejectReason('');
      fetchRequests();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to reject account request.');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h1 className="h3 mb-1">Account Opening Requests Queue</h1>
          <p className="text-muted mb-0 small">Review and approve or reject customer bank account requests</p>
        </div>
        <Button variant="outline-primary" size="sm" onClick={fetchRequests} disabled={loading}>
          <i className="bi bi-arrow-clockwise me-1" /> Refresh
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" role="status" />
            </div>
          ) : requests.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-check2-circle display-4 d-block mb-3 text-success" />
              <h5>No pending requests</h5>
              <p className="mb-0">All customer account opening requests have been processed.</p>
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Request ID</th>
                  <th>Customer Name</th>
                  <th>Account Type</th>
                  <th>Submission Date</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((req) => (
                  <tr key={req.id}>
                    <td>#{req.id}</td>
                    <td className="fw-semibold">{req.customerName}</td>
                    <td>{req.type}</td>
                    <td>{formatDate(req.requestedAt)}</td>
                    <td>
                      <Badge bg="warning" text="dark">Pending Review</Badge>
                    </td>
                    <td className="text-end">
                      <Button
                        variant="success"
                        size="sm"
                        className="me-2"
                        disabled={actionLoading}
                        onClick={() => handleApprove(req.id)}
                      >
                        <i className="bi bi-check-lg me-1" /> Approve
                      </Button>
                      <Button
                        variant="outline-danger"
                        size="sm"
                        disabled={actionLoading}
                        onClick={() => {
                          setSelectedRequest(req);
                          setShowRejectModal(true);
                        }}
                      >
                        <i className="bi bi-x-lg me-1" /> Reject
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      {/* Reject Reason Modal */}
      <Modal show={showRejectModal} onHide={() => !actionLoading && setShowRejectModal(false)} centered>
        <Form onSubmit={handleRejectSubmit}>
          <Modal.Header closeButton>
            <Modal.Title className="h5">Reject Account Request #{selectedRequest?.id}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p className="small text-muted mb-3">
              Please enter the mandatory reason for rejecting the account request for{' '}
              <strong>{selectedRequest?.customerName}</strong>. The customer will be notified with this explanation.
            </p>
            <Form.Group controlId="rejectReasonTextarea">
              <Form.Label className="fw-semibold">Rejection Reason</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="e.g. Identity documents incomplete or invalid address details."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                required
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowRejectModal(false)} disabled={actionLoading}>
              Cancel
            </Button>
            <Button variant="danger" type="submit" disabled={actionLoading || !rejectReason.trim()}>
              {actionLoading ? 'Rejecting...' : 'Confirm Rejection'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
