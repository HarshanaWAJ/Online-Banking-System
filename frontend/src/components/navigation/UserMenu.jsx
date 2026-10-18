import { NavDropdown } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import PATHS from '../../routes/routePaths.js';

/**
 * User menu dropdown in the top navbar.
 */
export default function UserMenu() {
  const { user, logout } = useAuth();

  return (
    <NavDropdown
      title={
        <span>
          <i className="bi bi-person-circle me-1" />
          {user?.email || 'Account'}
        </span>
      }
      align="end"
      id="user-menu-dropdown"
    >
      {user?.role === 'CUSTOMER' && (
        <NavDropdown.Item as={Link} to={PATHS.PROFILE}>
          <i className="bi bi-person me-2" />Profile
        </NavDropdown.Item>
      )}
      <NavDropdown.Item as={Link} to={PATHS.CHANGE_PASSWORD}>
        <i className="bi bi-key me-2" />Change Password
      </NavDropdown.Item>
      <NavDropdown.Divider />
      <NavDropdown.Item onClick={logout} className="text-danger">
        <i className="bi bi-box-arrow-right me-2" />Logout
      </NavDropdown.Item>
    </NavDropdown>
  );
}
