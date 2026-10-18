import { Badge, Container, Nav, Navbar } from 'react-bootstrap';
import { Link, Outlet } from 'react-router-dom';
import DemoBanner from '../components/feedback/DemoBanner.jsx';
import NotificationBell from '../components/navigation/NotificationBell.jsx';
import UserMenu from '../components/navigation/UserMenu.jsx';
import PATHS from '../routes/routePaths.js';

/**
 * Layout for authenticated STAFF users.
 */
export default function StaffLayout() {
  return (
    <div className="min-vh-100 d-flex flex-column bg-light">
      <Navbar bg="dark" variant="dark" expand="lg" className="shadow-sm">
        <Container>
          <Navbar.Brand as={Link} to={PATHS.STAFF_ACCOUNT_REQUESTS} className="fw-bold">
            <i className="bi bi-shield-check me-2" />
            Apex Bank <Badge bg="info" className="ms-1 text-dark">Staff</Badge>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="staff-navbar-nav" />
          <Navbar.Collapse id="staff-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} to={PATHS.STAFF_ACCOUNT_REQUESTS}>Account Requests</Nav.Link>
              <Nav.Link as={Link} to={PATHS.STAFF_CUSTOMERS}>Customers</Nav.Link>
              <Nav.Link as={Link} to={PATHS.STAFF_CASH_OPERATIONS}>Cash Operations</Nav.Link>
              <Nav.Link as={Link} to={PATHS.STAFF_SERVICE_REQUESTS}>Service Requests</Nav.Link>
              <Nav.Link as={Link} to={PATHS.STAFF_REPORTS}>Reports</Nav.Link>
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
