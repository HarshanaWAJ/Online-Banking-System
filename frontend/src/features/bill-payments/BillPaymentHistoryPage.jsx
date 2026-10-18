import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, Col, Row, Spinner, Table } from 'react-bootstrap';
import { billersApi } from '../../api/billers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDateTime } from '../../utils/formatDate.js';
import PATHS from '../../routes/routePaths.js';

export default function BillPaymentHistoryPage() {
  const { showError } = useToast();

  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const res = await billersApi.getMyBillPayments({ page, size: 15 });
      setPayments(res.data.data?.content || []);
      setTotalPages(res.data.data?.totalPages || 1);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load bill payments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, [page]);

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">Bill Payment History</h2>
          <p className="text-muted mb-0">Records of all utility bills paid through your accounts</p>
        </div>
        <Button as={Link} to={PATHS.PAY_BILL} variant="primary">
          <i className="bi bi-credit-card me-1"></i> Pay New Bill
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : payments.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-receipt-cutoff fs-1 d-block mb-2"></i>
              No bill payments found.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Date & Ref</th>
                    <th>Account</th>
                    <th>Biller</th>
                    <th>Subscriber Ref</th>
                    <th className="text-end">Amount</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {payments.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <div className="fw-semibold"><code>{p.reference}</code></div>
                        <div className="text-muted small">{formatDateTime(p.timestamp)}</div>
                      </td>
                      <td>
                        <code>{p.sourceAccountNumber}</code>
                      </td>
                      <td>
                        <div className="fw-bold text-primary">{p.billerName}</div>
                        <div className="text-muted small">{p.billerCategory}</div>
                      </td>
                      <td>
                        <code>{p.subscriberReference}</code>
                      </td>
                      <td className="text-end fw-bold text-danger">
                        - {formatMoney(p.amount)}
                      </td>
                      <td>
                        <Badge bg="success">PAID</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>

        {totalPages > 1 && (
          <Card.Footer className="bg-white d-flex justify-content-between align-items-center py-3">
            <Button
              variant="outline-secondary"
              size="sm"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="small text-muted">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline-secondary"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </Card.Footer>
        )}
      </Card>
    </div>
  );
}
