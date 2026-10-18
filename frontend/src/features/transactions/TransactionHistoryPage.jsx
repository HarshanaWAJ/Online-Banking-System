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

/**
 * ============================================================================
 * TransactionHistoryPage Component
 * ============================================================================
 * 
 * Purpose:
 * Provides a comprehensive, immutable ledger view for a specific bank account.
 * Displays real-time account balances, filtering by date range, paginated
 * transaction histories, and instant statement export (PDF and CSV formats).
 *
 * Key Architectural Highlights:
 * 1. Multi-role Access: Accessible by Customers (viewing their own account),
 *    Staff, and Admins (auditing accounts).
 * 2. Immutable Ledger Concept: Every transaction record originates from an
 *    append-only ledger entry with a running 'balanceAfter' snapshot.
 * 3. Export Capabilities: Supports on-the-fly binary Blob streaming for PDF
 *    and CSV statement generation.
 * ============================================================================
 */
export default function TransactionHistoryPage() {
  // Extract account identifier from the URL parameter (/accounts/:id/transactions)
  const { id } = useParams();

  // Toast notification hooks for positive confirmations and error feedback
  const { showError, showSuccess } = useToast();

  // --------------------------------------------------------------------------
  // Component State
  // --------------------------------------------------------------------------
  
  // Stores account metadata (account number, type, currency, active balance)
  const [account, setAccount] = useState(null);

  // Array of ledger entry items returned for the current page/filter
  const [transactions, setTransactions] = useState([]);

  // Loading flag for initial data loading and page transitions
  const [loading, setLoading] = useState(true);

  // Loading flag specifically for asynchronous statement generation & download
  const [downloading, setDownloading] = useState(false);

  // Filter state: ISO date strings selected by the user
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // Pagination state (0-indexed current page and total available pages)
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // --------------------------------------------------------------------------
  // Data Fetching: Account Summary & Paginated Transactions
  // --------------------------------------------------------------------------
  /**
   * Fetches both the account overview (for the summary header) and the
   * filtered ledger entries (for the transaction table).
   */
  const fetchAccountAndTransactions = async () => {
    try {
      setLoading(true);

      // 1. Fetch account details (balance, type, status, owner)
      const accRes = await accountApi.getAccountById(id);
      setAccount(accRes.data.data);

      // 2. Build query parameters for transaction listing
      const params = {
        page,
        size: 15, // Display 15 entries per page for optimal readability
      };

      // Convert local date inputs to standard UTC ISO strings if provided
      if (fromDate) {
        params.from = new Date(fromDate).toISOString();
      }
      if (toDate) {
        const toD = new Date(toDate);
        // Include full day up to 23:59:59.999
        toD.setHours(23, 59, 59, 999);
        params.to = toD.toISOString();
      }

      // 3. Request ledger history from the backend
      const txRes = await transactionsApi.getTransactions(id, params);

      // 4. Safely unpack data whether returned as a list or wrapped in Page object
      const list = Array.isArray(txRes.data.data)
        ? txRes.data.data
        : txRes.data.data?.content || [];
      setTransactions(list);

      // 5. Update pagination limits from server metadata
      setTotalPages(txRes.data.meta?.totalPages || txRes.data.data?.totalPages || 1);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load transaction history.');
    } finally {
      setLoading(false);
    }
  };

  // Re-fetch whenever account ID or the selected page changes
  useEffect(() => {
    fetchAccountAndTransactions();
  }, [id, page]);

  // --------------------------------------------------------------------------
  // User Actions: Filtering & Statement Exports
  // --------------------------------------------------------------------------
  /**
   * Resets page index to 0 and queries the backend with the new date boundaries.
   */
  const handleApplyFilter = (e) => {
    e.preventDefault();
    setPage(0);
    fetchAccountAndTransactions();
  };

  /**
   * Triggers statement export in either 'pdf' or 'csv' format.
   * Downloads the binary blob directly to the user's local disk.
   */
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

      // Invoke API statement generator
      await transactionsApi.downloadStatement(id, format, fromIso, toIso);
      showSuccess(`Statement downloaded successfully (${format.toUpperCase()}).`);
    } catch (err) {
      showError(err.apiError?.message || `Failed to download ${format.toUpperCase()} statement.`);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="container py-4">
      {/* ------------------------------------------------------------------ */}
      {/* Page Header & Quick Export Actions */}
      {/* ------------------------------------------------------------------ */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <Link to={PATHS.DASHBOARD} className="text-decoration-none text-muted small d-inline-block mb-1">
            <i className="bi bi-arrow-left me-1"></i> Back to Dashboard
          </Link>
          <h2 className="fw-bold mb-0">Account Statement & Transactions</h2>
        </div>

        {/* Export Buttons: Download PDF or CSV */}
        <div className="d-flex gap-2">
          <Button
            variant="outline-danger"
            onClick={() => handleDownloadStatement('pdf')}
            disabled={downloading || loading}
            title="Export official bank statement in PDF format"
          >
            {downloading ? (
              <Spinner size="sm" className="me-1" />
            ) : (
              <i className="bi bi-file-earmark-pdf me-1"></i>
            )}
            Download PDF
          </Button>
          <Button
            variant="outline-success"
            onClick={() => handleDownloadStatement('csv')}
            disabled={downloading || loading}
            title="Export spreadsheet-compatible CSV ledger entries"
          >
            {downloading ? (
              <Spinner size="sm" className="me-1" />
            ) : (
              <i className="bi bi-file-earmark-spreadsheet me-1"></i>
            )}
            Download CSV
          </Button>
        </div>
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Account Summary Banner */}
      {/* ------------------------------------------------------------------ */}
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
                <div className="fw-semibold">{account.type} Account</div>
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

      {/* ------------------------------------------------------------------ */}
      {/* Date Filter Toolbar */}
      {/* ------------------------------------------------------------------ */}
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

      {/* ------------------------------------------------------------------ */}
      {/* Ledger Transactions Table */}
      {/* ------------------------------------------------------------------ */}
      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-2 text-muted small">Loading transactions...</p>
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
                    <th>Date & Time</th>
                    <th>Reference</th>
                    <th>Description</th>
                    <th>Operation Type</th>
                    <th className="text-end">Amount (LKR)</th>
                    <th className="text-end">Running Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id}>
                      {/* Formatted Date & Time */}
                      <td className="small text-muted">{formatDateTime(tx.timestamp || tx.createdAt)}</td>

                      {/* Unique Transaction Reference */}
                      <td>
                        <code>{tx.reference}</code>
                        {tx.compensatesEntryId && (
                          <Badge bg="danger" className="ms-2">Reversal</Badge>
                        )}
                      </td>

                      {/* Transaction Note / Description */}
                      <td>
                        <div className="small fw-semibold">{tx.description}</div>
                      </td>

                      {/* Credit vs Debit Operation Badge */}
                      <td>
                        <Badge bg={tx.direction === 'CREDIT' ? 'success' : 'secondary'} className="small">
                          {tx.operationType}
                        </Badge>
                      </td>

                      {/* Amount: Green (+) for Credit, Red (-) for Debit */}
                      <td className="text-end fw-bold">
                        <span className={tx.direction === 'CREDIT' ? 'text-success' : 'text-danger'}>
                          {tx.direction === 'CREDIT' ? '+' : '-'} {formatMoney(tx.amount)}
                        </span>
                      </td>

                      {/* Resulting Balance Snapshot */}
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

        {/* ------------------------------------------------------------------ */}
        {/* Pagination Controls */}
        {/* ------------------------------------------------------------------ */}
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
