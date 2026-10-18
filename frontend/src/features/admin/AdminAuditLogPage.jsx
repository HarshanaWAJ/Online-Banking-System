import { useEffect, useState } from 'react';
import { Badge, Button, Card, Col, Form, Row, Spinner, Table } from 'react-bootstrap';
import { auditApi } from '../../api/audit.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDateTime } from '../../utils/formatDate.js';

export default function AdminAuditLogPage() {
  const { showError } = useToast();

  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionFilter, setActionFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchAuditEvents = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20 };
      if (actionFilter) params.action = actionFilter;
      const res = await auditApi.getAuditEvents(params);
      setEvents(res.data.data || []);
      setTotalPages(res.data.meta?.totalPages || 1);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load audit logs.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEvents();
  }, [page, actionFilter]);

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">System Audit Trail</h2>
          <p className="text-muted mb-0">Immutable compliance logs recording all administrative, financial, and security actions</p>
        </div>
        <div className="d-flex gap-2">
          <Form.Control
            type="text"
            placeholder="Filter by action (e.g. LOGIN)"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            style={{ width: '220px' }}
          />
          <Button variant="outline-secondary" onClick={fetchAuditEvents}>
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
          ) : events.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-shield-check fs-1 d-block mb-2"></i>
              No audit records matching criteria.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Timestamp</th>
                    <th>Actor ID</th>
                    <th>Role</th>
                    <th>Action</th>
                    <th>Target Resource</th>
                    <th>Outcome</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map((e) => (
                    <tr key={e.id}>
                      <td className="small text-muted">{formatDateTime(e.createdAt)}</td>
                      <td><code>#{e.actorId || 'SYS'}</code></td>
                      <td>
                        <Badge bg={e.actorRole === 'ADMIN' ? 'dark' : e.actorRole === 'STAFF' ? 'primary' : 'secondary'}>
                          {e.actorRole || 'SYSTEM'}
                        </Badge>
                      </td>
                      <td className="fw-semibold"><code>{e.action}</code></td>
                      <td>
                        {e.targetType ? `${e.targetType} #${e.targetId}` : '—'}
                      </td>
                      <td>
                        <Badge bg={e.outcome === 'SUCCESS' ? 'success' : 'danger'}>
                          {e.outcome}
                        </Badge>
                      </td>
                      <td className="small">{e.description}</td>
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
