import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Badge, Button, Card, Col, Form, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { adminUsersApi } from '../../api/adminUsers.api.js';
import { useToast } from '../../context/ToastContext.jsx';

const ALL_PERMISSIONS = [
  'APPROVE_ACCOUNTS',
  'CASH_OPERATIONS',
  'HANDLE_REQUESTS',
  'VIEW_REPORTS',
];

export default function AdminUsersPage() {
  const { showSuccess, showError } = useToast();

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showPermModal, setShowPermModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userPermissions, setUserPermissions] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      email: '',
      password: '',
      role: 'STAFF',
    },
  });

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await adminUsersApi.listUsers({ page: 0, size: 50 });
      setUsers(res.data.data?.content || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load system users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleOpenAdd = () => {
    reset({ email: '', password: '', role: 'STAFF' });
    setUserPermissions(['APPROVE_ACCOUNTS', 'CASH_OPERATIONS', 'HANDLE_REQUESTS', 'VIEW_REPORTS']);
    setShowAddModal(true);
  };

  const handleToggleStatus = async (u) => {
    try {
      await adminUsersApi.updateUserStatus(u.id, !u.active);
      showSuccess(`User ${u.email} is now ${!u.active ? 'active' : 'inactive'}.`);
      fetchUsers();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to update user status.');
    }
  };

  const handleOpenPermModal = (u) => {
    setSelectedUser(u);
    setUserPermissions(u.permissions || []);
    setShowPermModal(true);
  };

  const handlePermissionCheck = (perm) => {
    setUserPermissions((prev) =>
      prev.includes(perm) ? prev.filter((p) => p !== perm) : [...prev, perm]
    );
  };

  const onSubmitAdd = async (values) => {
    try {
      setSubmitting(true);
      const payload = {
        email: values.email.trim(),
        password: values.password,
        role: values.role,
        permissions: values.role === 'STAFF' ? userPermissions : [],
      };
      await adminUsersApi.createStaffUser(payload);
      showSuccess('Staff user created successfully.');
      setShowAddModal(false);
      fetchUsers();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to create user.');
    } finally {
      setSubmitting(false);
    }
  };

  const onSavePermissions = async () => {
    try {
      setSubmitting(true);
      await adminUsersApi.updateStaffPermissions(selectedUser.id, {
        permissions: userPermissions,
      });
      showSuccess('Permissions updated successfully.');
      setShowPermModal(false);
      fetchUsers();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to update permissions.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">System Users & Permissions</h2>
          <p className="text-muted mb-0">Manage internal banking staff, system administrators, and role-based permissions</p>
        </div>
        <Button variant="primary" onClick={handleOpenAdd}>
          <i className="bi bi-person-plus me-1"></i> Add Staff User
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : users.length === 0 ? (
            <div className="text-center py-5 text-muted">
              No users found.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>User ID</th>
                    <th>Email Address</th>
                    <th>Role</th>
                    <th>Permissions</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id}>
                      <td><code>#{u.id}</code></td>
                      <td className="fw-semibold">{u.email}</td>
                      <td>
                        <Badge bg={u.role === 'ADMIN' ? 'dark' : u.role === 'STAFF' ? 'primary' : 'secondary'}>
                          {u.role}
                        </Badge>
                      </td>
                      <td>
                        {u.role === 'STAFF' ? (
                          <div className="d-flex flex-wrap gap-1">
                            {(u.permissions || []).map((p) => (
                              <Badge key={p} bg="light" text="dark" className="border">
                                {p.replace('_', ' ')}
                              </Badge>
                            ))}
                          </div>
                        ) : u.role === 'ADMIN' ? (
                          <span className="small text-muted fst-italic">Full Administrative Access</span>
                        ) : (
                          <span className="small text-muted fst-italic">Customer Portal Access</span>
                        )}
                      </td>
                      <td>
                        <Badge bg={u.active ? 'success' : 'danger'}>
                          {u.active ? 'Active' : 'Inactive'}
                        </Badge>
                      </td>
                      <td className="text-end">
                        {u.role === 'STAFF' && (
                          <Button
                            variant="outline-secondary"
                            size="sm"
                            className="me-2"
                            onClick={() => handleOpenPermModal(u)}
                          >
                            Permissions
                          </Button>
                        )}
                        <Button
                          variant={u.active ? 'outline-danger' : 'outline-success'}
                          size="sm"
                          onClick={() => handleToggleStatus(u)}
                        >
                          {u.active ? 'Deactivate' : 'Activate'}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Card.Body>
      </Card>

      {/* Add Staff Modal */}
      <Modal show={showAddModal} onHide={() => setShowAddModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">Create System User</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit(onSubmitAdd)}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Email *</Form.Label>
              <Form.Control
                type="email"
                placeholder="staff@bankdemo.lk"
                {...register('email', { required: 'Email is required' })}
                isInvalid={!!errors.email}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Password *</Form.Label>
              <Form.Control
                type="password"
                placeholder="Minimum 8 characters"
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 8, message: 'Minimum 8 characters' },
                })}
                isInvalid={!!errors.password}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">System Role *</Form.Label>
              <Form.Select {...register('role', { required: true })}>
                <option value="STAFF">Staff Member</option>
                <option value="ADMIN">System Administrator</option>
              </Form.Select>
            </Form.Group>

            <div className="mb-3">
              <Form.Label className="fw-semibold">Staff Permissions (if Staff)</Form.Label>
              {ALL_PERMISSIONS.map((perm) => (
                <Form.Check
                  key={perm}
                  type="checkbox"
                  label={perm.replace('_', ' ')}
                  checked={userPermissions.includes(perm)}
                  onChange={() => handlePermissionCheck(perm)}
                />
              ))}
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowAddModal(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? <Spinner size="sm" className="me-2" /> : null}
              Create User
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Update Permissions Modal */}
      <Modal show={showPermModal} onHide={() => setShowPermModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">
            Edit Permissions: {selectedUser?.email}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="text-muted small">Select operations this staff user is authorized to perform:</p>
          {ALL_PERMISSIONS.map((perm) => (
            <Form.Check
              key={perm}
              type="checkbox"
              className="mb-2"
              label={<strong>{perm.replace('_', ' ')}</strong>}
              checked={userPermissions.includes(perm)}
              onChange={() => handlePermissionCheck(perm)}
            />
          ))}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowPermModal(false)} disabled={submitting}>
            Cancel
          </Button>
          <Button variant="primary" onClick={onSavePermissions} disabled={submitting}>
            {submitting ? <Spinner size="sm" className="me-2" /> : null}
            Save Permissions
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
