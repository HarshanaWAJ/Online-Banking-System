import { Container, Navbar } from 'react-bootstrap';
import { Outlet } from 'react-router-dom';
import DemoBanner from '../components/feedback/DemoBanner.jsx';

/**
 * Layout for public unauthenticated pages (Login, Register).
 */
export default function PublicLayout() {
  return (
    <div className="min-vh-100 d-flex flex-column bg-light">
      <Navbar bg="dark" variant="dark" expand="lg" className="shadow-sm">
        <Container>
          <Navbar.Brand className="fw-bold">
            <i className="bi bi-bank2 me-2" />
            Apex Bank PLC
          </Navbar.Brand>
        </Container>
      </Navbar>
      <DemoBanner />
      <div className="flex-grow-1 d-flex align-items-center justify-content-center py-5">
        <Container>
          <Outlet />
        </Container>
      </div>
    </div>
  );
}
