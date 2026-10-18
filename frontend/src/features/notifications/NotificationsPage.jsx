import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, ListGroup, Pagination, Spinner } from 'react-bootstrap';
import { notificationApi } from '../../api/notifications.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDateTime } from '../../utils/formatDate.js';

export default function NotificationsPage() {
  const { showSuccess, showError } = useToast();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchNotifications = async (targetPage = 0) => {
    try {
      setLoading(true);
      const res = await notificationApi.getNotifications({ page: targetPage, size: 15 });
      const pageData = res.data.data || [];
      const meta = res.data.meta || {};
      setNotifications(pageData);
      setPage(meta.page || 0);
      setTotalPages(meta.totalPages || 0);
      setTotalElements(meta.totalElements || 0);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load notifications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications(page);
  }, [page]);

  const handleMarkAsRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      showSuccess('Notification marked as read.');
    } catch (err) {
      showError(err.apiError?.message || 'Could not update notification.');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      setActionLoading(true);
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      showSuccess('All notifications marked as read.');
    } catch (err) {
      showError(err.apiError?.message || 'Failed to mark all as read.');
    } finally {
      setActionLoading(false);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'ACCOUNT_APPROVED':
      case 'ACCOUNT_OPENED':
        return <i className="bi bi-bank text-success fs-5 me-3" />;
      case 'TRANSFER_SENT':
      case 'TRANSFER_RECEIVED':
        return <i className="bi bi-arrow-left-right text-primary fs-5 me-3" />;
      case 'BILL_PAID':
        return <i className="bi bi-receipt text-info fs-5 me-3" />;
      case 'SERVICE_REQUEST_UPDATE':
        return <i className="bi bi-headset text-warning fs-5 me-3" />;
      case 'SECURITY_ALERT':
        return <i className="bi bi-shield-exclamation text-danger fs-5 me-3" />;
      default:
        return <i className="bi bi-bell text-secondary fs-5 me-3" />;
    }
  };

  return (
    <div className="container-fluid py-2">
      <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h2 className="h4 fw-bold mb-1">Notifications</h2>
          <p className="text-muted small mb-0">
            Account alerts, transaction confirmations, and security notices
          </p>
        </div>
        <div className="d-flex gap-2">
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => fetchNotifications(page)}
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise me-1" />
            Refresh
          </Button>
          <Button
            variant="outline-primary"
            size="sm"
            onClick={handleMarkAllRead}
            disabled={actionLoading || notifications.length === 0}
          >
            <i className="bi bi-check2-all me-1" />
            Mark All as Read
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <Spinner animation="border" variant="primary" role="status" />
          <p className="mt-2 text-muted small">Loading notifications...</p>
        </div>
      ) : notifications.length === 0 ? (
        <Card className="text-center p-5 shadow-sm border-0 bg-light">
          <Card.Body>
            <i className="bi bi-bell-slash fs-1 text-muted d-block mb-3" />
            <h5 className="fw-bold">No Notifications</h5>
            <p className="text-muted small mb-0">
              You are all caught up! There are no pending alerts or announcements.
            </p>
          </Card.Body>
        </Card>
      ) : (
        <Card className="shadow-sm">
          <ListGroup variant="flush">
            {notifications.map((item) => (
              <ListGroup.Item
                key={item.id}
                className={`p-3 d-flex align-items-center justify-content-between ${
                  !item.read ? 'bg-light border-start border-4 border-primary' : ''
                }`}
              >
                <div className="d-flex align-items-start me-3">
                  {getNotificationIcon(item.type)}
                  <div>
                    <div className="d-flex align-items-center gap-2 mb-1">
                      <span className="fw-semibold text-dark">{item.message}</span>
                      {!item.read && (
                        <Badge bg="primary" pill>
                          New
                        </Badge>
                      )}
                    </div>
                    <div className="text-muted small">
                      {formatDateTime(item.createdAt)}
                    </div>
                  </div>
                </div>

                {!item.read && (
                  <Button
                    variant="outline-secondary"
                    size="sm"
                    className="flex-shrink-0"
                    onClick={() => handleMarkAsRead(item.id)}
                    title="Mark as read"
                  >
                    <i className="bi bi-check2" />
                  </Button>
                )}
              </ListGroup.Item>
            ))}
          </ListGroup>

          {totalPages > 1 && (
            <Card.Footer className="bg-white d-flex justify-content-between align-items-center py-2">
              <span className="text-muted small">
                Showing {notifications.length} of {totalElements} notifications
              </span>
              <Pagination className="mb-0" size="sm">
                <Pagination.Prev
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(p - 1, 0))}
                />
                {Array.from({ length: totalPages }, (_, idx) => (
                  <Pagination.Item
                    key={idx}
                    active={idx === page}
                    onClick={() => setPage(idx)}
                  >
                    {idx + 1}
                  </Pagination.Item>
                ))}
                <Pagination.Next
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                />
              </Pagination>
            </Card.Footer>
          )}
        </Card>
      )}
    </div>
  );
}
