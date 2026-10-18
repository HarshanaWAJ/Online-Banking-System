import { useEffect, useState } from 'react';
import { Button, Card, Col, Row, Spinner } from 'react-bootstrap';
import { reportsApi } from '../../api/reports.api.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function ReportsDashboardPage() {
  const { showSuccess, showError } = useToast();

  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloadingAccounts, setDownloadingAccounts] = useState(false);
  const [downloadingTx, setDownloadingTx] = useState(false);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const res = await reportsApi.getOverview();
        setStats(res.data.data);
      } catch (err) {
        showError(err.apiError?.message || 'Failed to load report metrics.');
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const handleDownloadAccounts = async () => {
    try {
      setDownloadingAccounts(true);
      await reportsApi.downloadAccountsCsv();
      showSuccess('Accounts CSV report downloaded.');
    } catch (err) {
      showError('Failed to download accounts CSV.');
    } finally {
      setDownloadingAccounts(false);
    }
  };

  const handleDownloadTx = async () => {
    try {
      setDownloadingTx(true);
      await reportsApi.downloadTransactionsCsv();
      showSuccess('Transactions CSV report downloaded.');
    } catch (err) {
      showError('Failed to download transactions CSV.');
    } finally {
      setDownloadingTx(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="mb-4">
        <h2 className="fw-bold mb-1">Operational Reports & Analytics</h2>
        <p className="text-muted mb-0">System-wide performance indicators and compliance CSV exports</p>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" />
        </div>
      ) : (
        <>
          {/* Metrics Row */}
          <Row className="g-4 mb-4">
            <Col sm={6} lg={3}>
              <Card className="shadow-sm border-0 border-start border-primary border-4">
                <Card.Body className="p-3">
                  <div className="text-muted small text-uppercase fw-bold">Total Customers</div>
                  <h3 className="fw-bold mt-2 mb-0">{stats?.totalCustomers || 0}</h3>
                </Card.Body>
              </Card>
            </Col>

            <Col sm={6} lg={3}>
              <Card className="shadow-sm border-0 border-start border-success border-4">
                <Card.Body className="p-3">
                  <div className="text-muted small text-uppercase fw-bold">Bank Accounts</div>
                  <h3 className="fw-bold mt-2 mb-0">{stats?.totalAccounts || 0}</h3>
                </Card.Body>
              </Card>
            </Col>

            <Col sm={6} lg={3}>
              <Card className="shadow-sm border-0 border-start border-warning border-4">
                <Card.Body className="p-3">
                  <div className="text-muted small text-uppercase fw-bold">Pending Accounts</div>
                  <h3 className="fw-bold mt-2 mb-0 text-warning">{stats?.pendingAccountRequests || 0}</h3>
                </Card.Body>
              </Card>
            </Col>

            <Col sm={6} lg={3}>
              <Card className="shadow-sm border-0 border-start border-info border-4">
                <Card.Body className="p-3">
                  <div className="text-muted small text-uppercase fw-bold">Support Tickets</div>
                  <h3 className="fw-bold mt-2 mb-0">{stats?.totalServiceTickets || 0}</h3>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Export Center Cards */}
          <h4 className="fw-bold mb-3">Regulatory & Operational Data Exports</h4>
          <Row className="g-4">
            <Col md={6}>
              <Card className="shadow-sm border-0 h-100">
                <Card.Body className="p-4 d-flex flex-column justify-content-between">
                  <div>
                    <h5 className="fw-bold text-primary">
                      <i className="bi bi-file-earmark-spreadsheet me-2"></i>Accounts Register Export
                    </h5>
                    <p className="text-muted">
                      Full snapshot of all customer bank accounts, including balances, statuses,
                      account types, and creation dates.
                    </p>
                  </div>
                  <Button
                    variant="outline-primary"
                    className="fw-bold mt-3"
                    onClick={handleDownloadAccounts}
                    disabled={downloadingAccounts}
                  >
                    {downloadingAccounts ? <Spinner size="sm" className="me-2" /> : null}
                    Download Accounts CSV
                  </Button>
                </Card.Body>
              </Card>
            </Col>

            <Col md={6}>
              <Card className="shadow-sm border-0 h-100">
                <Card.Body className="p-4 d-flex flex-column justify-content-between">
                  <div>
                    <h5 className="fw-bold text-success">
                      <i className="bi bi-journal-arrow-down me-2"></i>Transactions Ledger Export
                    </h5>
                    <p className="text-muted">
                      Complete double-entry audit records of all debits, credits, transfers,
                      cash operations, and bill payments across the bank.
                    </p>
                  </div>
                  <Button
                    variant="outline-success"
                    className="fw-bold mt-3"
                    onClick={handleDownloadTx}
                    disabled={downloadingTx}
                  >
                    {downloadingTx ? <Spinner size="sm" className="me-2" /> : null}
                    Download Transactions CSV
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
}
