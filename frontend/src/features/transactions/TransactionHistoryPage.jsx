import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Badge,
  Button,
  Card,
  Col,
  Form,
  Row,
  Spinner,
  Table,
} from 'react-bootstrap';
import { transactionsApi } from '../../api/transactions.api.js';
import { accountApi } from '../../api/accounts.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDateTime } from '../../utils/formatDate.js';
import PATHS from '../../routes/routePaths.js';

export default function TransactionHistoryPage() {
  const { id } = useParams();
  const { showError, showSuccess } = useToast();

  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  // Date filters
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchAccountAndTransactions = async () => {
    try {
      setLoading(true);
      const accRes = await accountApi.getAccountById(id);
      setAccount(accRes.data.data);

      const params = {
        page,
        size: 15,
      };
      if (fromDate) params.from = new Date(fromDate).toISOString();
      if (toDate) {
        const toD = new Date(toDate);
        toD.setHours(23, 59, 59, 999);
        params.to = toD.toISOString();
      }

      const txRes = await transactionsApi.getTransactions(id, params);
      const list = Array.isArray(txRes.data.data)
        ? txRes.data.data
        : txRes.data.data?.content || [];
      setTransactions(list);
      setTotalPages(txRes.data.meta?.totalPages || txRes.data.data?.totalPages || 1);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load transaction history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccountAndTransactions();
  }, [id, page]);

  const handleApplyFilter = (e) => {
    e.preventDefault();
    setPage(0);
    fetchAccountAndTransactions();
  };

  const handleDownloadStatement = async (format) => {
    try {
      setDownloading(true);
      const fromIso = fromDate ? new Date(fromDate).toISOString() : undefined;
      let toIso;
      if (toDate) {
        const toD = new Date(toDate);
        toD.setHours(23, 59, 59, 999);
        toIso = toD.toISOString();
      }
      await transactionsApi.downloadStatement(id, format, fromIso, toIso);
      showSuccess(`Statement downloaded (${format.toUpperCase()}).`);
    } catch (err) {
      showError(err.apiError?.message || `Failed to download ${format.toUpperCase()} statement.`);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="container py-4">
      {/* Back button & Page Title */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <Link to={PATHS.DASHBOARD} className="text-decoration-none text-muted small d-inline-block mb-1">
            <i className="bi bi-arrow-left me-1"></i> Back to Dashboard
          </Link>
          <h2 className="fw-bold mb-0">Account Statement & Transactions</h2>
        </div>

        {/* Export Buttons */}
        <div className="d-flex gap-2">
          <Button
            variant="outline-danger"
            onClick={() => handleDownloadStatement('pdf')}
            disabled={downloading || loading}
          >
            <i className="bi bi-file-earmark-pdf me-1"></i> Download PDF
          </Button>
          <Button
            variant="outline-success"
            onClick={() => handleDownloadStatement('csv')}
            disabled={downloading || loading}
          >
            <i className="bi bi-file-earmark-spreadsheet me-1"></i> Download CSV
          </Button>
        </div>
      </div>

      {/* Account Info Header Card */}
      {account && (
        <Card className="shadow-sm border-0 mb-4 bg-light">
          <Card.Body className="py-3 px-4">
            <Row className="align-items-center">
              <Col md={3}>
                <div className="text-muted small">Account Number</div>
                <div className="fs-5 fw-bold"><code>{account.accountNumber}</code></div>
              </Col>
              <Col md={3}>
                <div className="text-muted small">Account Type</div>
                <div className="fw-semibold">{account.type}</div>
              </Col>
              <Col md={3}>
                <div className="text-muted small">Account Status</div>
                <div>
                  <Badge bg={account.status === 'ACTIVE' ? 'success' : 'secondary'}>
                    {account.status}
                  </Badge>
                </div>
              </Col>
              <Col md={3} className="text-md-end">
                <div className="text-muted small">Available Balance</div>
                <div className="fs-4 fw-bold text-primary">
                  {formatMoney(account.balance, account.currency)}
                </div>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      )}

      {/* Filter Toolbar */}
      <Card className="shadow-sm border-0 mb-4">
        <Card.Body>
          <Form onSubmit={handleApplyFilter} className="row g-3 align-items-end">
            <Col md={4}>
              <Form.Label className="small fw-semibold text-muted">From Date</Form.Label>
              <Form.Control
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
            </Col>
            <Col md={4}>
              <Form.Label className="small fw-semibold text-muted">To Date</Form.Label>
              <Form.Control
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
            </Col>
            <Col md={4} className="d-flex gap-2">
              <Button variant="primary" type="submit" className="w-100" disabled={loading}>
                <i className="bi bi-funnel me-1"></i> Apply Filter
              </Button>
              <Button
                variant="outline-secondary"
                type="button"
                onClick={() => {
                  setFromDate('');
                  setToDate('');
                  setPage(0);
                  fetchAccountAndTransactions();
                }}
                disabled={loading}
              >
                Reset
              </Button>
            </Col>
          </Form>
        </Card.Body>
      </Card>

      {/* Ledger Table */}
      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : transactions.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-journal-x fs-1 d-block mb-2"></i>
              No transactions recorded for this period.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Date (UTC)</th>
                    <th>Reference</th>
                    <th>Details</th>
                    <th>Type</th>
                    <th className="text-end">Amount</th>
                    <th className="text-end">Balance After</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id}>
                      <td className="small text-muted">{formatDateTime(tx.timestamp)}</td>
                      <td>
                        <code>{tx.reference}</code>
                        {tx.compensatesEntryId && (
                          <Badge bg="danger" className="ms-2">Reversal</Badge>
                        )}
                      </td>
                      <td>
                        <div className="small fw-semibold">{tx.description}</div>
                      </td>
                      <td>
                        <Badge bg={tx.direction === 'CREDIT' ? 'success' : 'secondary'} className="small">
                          {tx.operationType}
                        </Badge>
                      </td>
                      <td className="text-end fw-bold">
                        <span className={tx.direction === 'CREDIT' ? 'text-success' : 'text-danger'}>
                          {tx.direction === 'CREDIT' ? '+' : '-'} {formatMoney(tx.amount)}
                        </span>
                      </td>
                      <td className="text-end fw-semibold">
                        {formatMoney(tx.balanceAfter)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>

        {/* Pagination Footer */}
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
