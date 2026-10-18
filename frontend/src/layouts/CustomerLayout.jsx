import { Container, Nav, Navbar, NavDropdown } from 'react-bootstrap';
import { Link, Outlet } from 'react-router-dom';
import DemoBanner from '../components/feedback/DemoBanner.jsx';
import NotificationBell from '../components/navigation/NotificationBell.jsx';
import UserMenu from '../components/navigation/UserMenu.jsx';
import PATHS from '../routes/routePaths.js';

/**
 * Layout for authenticated CUSTOMER users.
 */
export default function CustomerLayout() {
  return (
    <div className="min-vh-100 d-flex flex-column bg-light">
      <Navbar bg="primary" variant="dark" expand="lg" className="shadow-sm">
        <Container>
          <Navbar.Brand as={Link} to={PATHS.DASHBOARD} className="fw-bold">
            <i className="bi bi-bank2 me-2" />
            Apex Bank
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="customer-navbar-nav" />
          <Navbar.Collapse id="customer-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} to={PATHS.DASHBOARD}>Dashboard</Nav.Link>
              <NavDropdown title="Accounts" id="accounts-nav-dropdown">
                <NavDropdown.Item as={Link} to={PATHS.DASHBOARD}>My Accounts</NavDropdown.Item>
                <NavDropdown.Item as={Link} to={PATHS.REQUEST_ACCOUNT}>Request Account</NavDropdown.Item>
              </NavDropdown>
              <Nav.Link as={Link} to={PATHS.TRANSFER}>Transfer</Nav.Link>
              <Nav.Link as={Link} to={PATHS.PAY_BILL}>Pay Bills</Nav.Link>
              <Nav.Link as={Link} to={PATHS.BENEFICIARIES}>Beneficiaries</Nav.Link>
              <Nav.Link as={Link} to={PATHS.SERVICE_REQUESTS}>Service Requests</Nav.Link>
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
