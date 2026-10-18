import { Badge, Container, Nav, Navbar } from 'react-bootstrap';
import { Link, Outlet } from 'react-router-dom';
import DemoBanner from '../components/feedback/DemoBanner.jsx';
import NotificationBell from '../components/navigation/NotificationBell.jsx';
import UserMenu from '../components/navigation/UserMenu.jsx';
import PATHS from '../routes/routePaths.js';

/**
 * Layout for authenticated ADMIN users.
 */
export default function AdminLayout() {
  return (
    <div className="min-vh-100 d-flex flex-column bg-light">
      <Navbar bg="secondary" variant="dark" expand="lg" className="shadow-sm">
        <Container>
          <Navbar.Brand as={Link} to={PATHS.ADMIN_USERS} className="fw-bold">
            <i className="bi bi-shield-lock-fill me-2" />
            Apex Bank <Badge bg="danger" className="ms-1">Admin</Badge>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="admin-navbar-nav" />
          <Navbar.Collapse id="admin-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} to={PATHS.ADMIN_USERS}>Users</Nav.Link>
              <Nav.Link as={Link} to={PATHS.ADMIN_BILLERS}>Billers</Nav.Link>
              <Nav.Link as={Link} to={PATHS.ADMIN_REPORTS}>Reports</Nav.Link>
              <Nav.Link as={Link} to={PATHS.ADMIN_AUDIT}>Audit Trail</Nav.Link>
              <Nav.Link as={Link} to={PATHS.ADMIN_SETTINGS}>Settings</Nav.Link>
            </Nav>
            <Nav className="align-items-center">
              <NotificationBell />
              <UserMenu />
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <DemoBanner />
      <main className="flex-grow-1 py-4">
        <Container>
          <Outlet />
        </Container>
      </main>
    </div>
  );
}
