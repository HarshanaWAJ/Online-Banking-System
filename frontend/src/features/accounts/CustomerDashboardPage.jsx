import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Row, Spinner, Table } from 'react-bootstrap';
import { accountApi } from '../../api/accounts.api.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDate } from '../../utils/formatDate.js';

export default function CustomerDashboardPage() {
  const { user } = useAuth();
  const { showError, showSuccess } = useToast();

  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const res = await accountApi.getMyAccounts();
      setAccounts(res.data.data || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load your accounts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleCancelRequest = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this pending account request?')) {
      return;
    }
    try {
      setCancellingId(id);
      await accountApi.cancelAccountRequest(id);
      showSuccess('Account request cancelled.');
      fetchAccounts();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to cancel request.');
    } finally {
      setCancellingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge bg="success">Active</Badge>;
      case 'PENDING':
        return <Badge bg="warning" text="dark">Pending Approval</Badge>;
      case 'FROZEN':
        return <Badge bg="danger">Frozen</Badge>;
      case 'CLOSED':
        return <Badge bg="secondary">Closed</Badge>;
      default:
        return <Badge bg="light" text="dark">{status}</Badge>;
    }
  };

  const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE');
  const totalBalance = activeAccounts.reduce((acc, curr) => acc + (parseFloat(curr.balance) || 0), 0);

  return (
    <div>
      {/* Welcome & Overview Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h1 className="h3 mb-1">Welcome back, {user?.email}</h1>
          <p className="text-muted mb-0 small">Overview of your bank accounts and simulated assets</p>
        </div>
        <div className="mt-2 mt-sm-0">
          <Button as={Link} to={PATHS.REQUEST_ACCOUNT} variant="primary" size="sm">
            <i className="bi bi-plus-circle me-1" /> Request New Account
          </Button>
        </div>
      </div>

      {/* Aggregate Balance Card */}
      <Row className="mb-4">
        <Col md={6} lg={4}>
          <Card className="shadow-sm border-0 bg-primary text-white">
            <Card.Body className="p-3">
              <span className="text-white-50 small text-uppercase fw-bold">Combined Active Balance</span>
              <h2 className="display-6 fw-bold mt-1 mb-0">{formatMoney(totalBalance)}</h2>
              <span className="small text-white-50">Across {activeAccounts.length} active account(s)</span>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Accounts List Table */}
      <Card className="shadow-sm border-0 mb-4">
        <Card.Header className="bg-white py-3 d-flex justify-content-between align-items-center">
          <h5 className="mb-0 fw-semibold">Your Accounts</h5>
          <span className="text-muted small">{accounts.length} total</span>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" role="status" />
            </div>
          ) : accounts.length === 0 ? (
            <div className="text-center py-5">
              <i className="bi bi-wallet2 display-4 text-muted d-block mb-3" />
              <h5>No accounts found</h5>
              <p className="text-muted mb-3">You do not have any bank accounts yet.</p>
              <Button as={Link} to={PATHS.REQUEST_ACCOUNT} variant="outline-primary" size="sm">
                Request an Account
              </Button>
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Account Number</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th className="text-end">Balance</th>
                  <th>Requested Date</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((acc) => (
                  <tr key={acc.id}>
                    <td>
                      {acc.accountNumber ? (
                        <span className="fw-bold font-monospace">{acc.accountNumber}</span>
                      ) : (
                        <span className="text-muted fst-italic">Pending Assignment</span>
                      )}
                    </td>
                    <td>{acc.type}</td>
                    <td>{getStatusBadge(acc.status)}</td>
                    <td className="text-end fw-semibold">
                      {acc.status === 'ACTIVE' || acc.status === 'FROZEN' ? formatMoney(acc.balance) : '—'}
                    </td>
                    <td>{formatDate(acc.requestedAt)}</td>
                    <td className="text-end">
                      {acc.status === 'PENDING' && (
                        <Button
                          variant="outline-danger"
                          size="sm"
                          disabled={cancellingId === acc.id}
                          onClick={() => handleCancelRequest(acc.id)}
                        >
                          {cancellingId === acc.id ? 'Cancelling...' : 'Cancel'}
                        </Button>
                      )}
                      {acc.status === 'ACTIVE' && (
                        <Button
                          as={Link}
                          to={`/accounts/${acc.id}/transactions`}
                          variant="outline-primary"
                          size="sm"
                          className="me-2"
                        >
                          <i className="bi bi-receipt me-1"></i> Statement
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}
