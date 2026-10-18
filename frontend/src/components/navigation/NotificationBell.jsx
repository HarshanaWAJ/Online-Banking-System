import { useEffect, useState } from 'react';
import { Badge, Nav } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { notificationApi } from '../../api/notifications.api.js';
import PATHS from '../../routes/routePaths.js';

/**
 * Notification bell icon linking to notifications page with live unread count badge.
 */
export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchCount = async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      setUnreadCount(res.data.data || 0);
    } catch {
      // Silently ignore if unauthenticated or network failure
    }
  };

  useEffect(() => {
    fetchCount();
    const interval = setInterval(fetchCount, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Nav.Link
      as={Link}
      to={PATHS.NOTIFICATIONS}
      className="position-relative px-2 me-2"
      aria-label="Notifications"
    >
      <i className="bi bi-bell fs-5" />
      {unreadCount > 0 && (
        <Badge
          pill
          bg="danger"
          className="position-absolute top-0 start-100 translate-middle"
          style={{ fontSize: '0.65rem' }}
        >
          {unreadCount > 99 ? '99+' : unreadCount}
        </Badge>
      )}
      <span className="visually-hidden">Notifications</span>
    </Nav.Link>
  );
}
