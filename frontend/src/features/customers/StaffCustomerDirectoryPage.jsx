import { useEffect, useState } from 'react';
import { Badge, Button, Card, Col, Form, InputGroup, Modal, Row, Spinner, Table } from 'react-bootstrap';
import { customerApi } from '../../api/customers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDate } from '../../utils/formatDate.js';

export default function StaffCustomerDirectoryPage() {
  const { showError, showSuccess } = useToast();

  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);

  const fetchCustomers = async (searchTerm = '') => {
    try {
      setLoading(true);
      const res = await customerApi.searchCustomers({ search: searchTerm, page: 0, size: 50 });
      setCustomers(res.data.data || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load customer directory.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomers();
  }, []);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchCustomers(search);
  };

  const handleViewCustomer = (c) => {
    setSelectedCustomer(c);
    setShowDetailModal(true);
  };

  const handleToggleStatus = async (c) => {
    try {
      const newStatus = !c.active;
      await customerApi.toggleCustomerStatus(c.id, newStatus);
      showSuccess(`Customer ${newStatus ? 'activated' : 'deactivated'} successfully.`);
      fetchCustomers(search);
      if (selectedCustomer && selectedCustomer.id === c.id) {
        setSelectedCustomer((prev) => ({ ...prev, active: newStatus }));
      }
    } catch (err) {
      showError(err.apiError?.message || 'Failed to update customer status.');
    }
  };

  return (
    <div className="container-fluid py-2">
      <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h2 className="h4 fw-bold mb-1">Customer Directory</h2>
          <p className="text-muted small mb-0">
            Search, view KYC verification records, and manage registered bank customers
          </p>
        </div>
      </div>

      <Card className="shadow-sm mb-4">
        <Card.Body>
          <Form onSubmit={handleSearchSubmit}>
            <Row className="g-2">
              <Col md={8}>
                <InputGroup>
                  <InputGroup.Text>
                    <i className="bi bi-search" />
                  </InputGroup.Text>
                  <Form.Control
                    type="text"
                    placeholder="Search by full name, NIC number, email, or mobile..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                  />
                </InputGroup>
              </Col>
              <Col md={4} className="d-flex gap-2">
                <Button variant="primary" type="submit" disabled={loading} className="w-100">
                  Search
                </Button>
                {search && (
                  <Button
                    variant="outline-secondary"
                    onClick={() => {
                      setSearch('');
                      fetchCustomers('');
                    }}
                  >
                    Clear
                  </Button>
                )}
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      <Card className="shadow-sm">
        <Card.Header className="bg-white py-3 fw-semibold">
          Customer Records ({customers.length})
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" role="status" />
              <p className="mt-2 text-muted small">Loading customers...</p>
            </div>
          ) : customers.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-people fs-1 d-block mb-2" />
              No customer records found matching your search.
            </div>
          ) : (
            <Table responsive hover className="mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Customer Name</th>
                  <th>NIC Number</th>
                  <th>Email Address</th>
                  <th>Phone</th>
                  <th>Date of Birth</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((c) => (
                  <tr key={c.id}>
                    <td className="fw-semibold">
                      {c.firstName} {c.lastName}
                    </td>
                    <td>
                      <code className="text-dark">{c.nic}</code>
                    </td>
                    <td>{c.email}</td>
                    <td>{c.phone || '—'}</td>
                    <td>{formatDate(c.dateOfBirth)}</td>
                    <td>
                      {c.active ? (
                        <Badge bg="success">Active</Badge>
                      ) : (
                        <Badge bg="secondary">Inactive</Badge>
                      )}
                    </td>
                    <td className="text-end">
                      <Button
                        variant="outline-primary"
                        size="sm"
                        onClick={() => handleViewCustomer(c)}
                      >
                        <i className="bi bi-eye me-1" />
                        Details
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      {/* Customer Detail Modal */}
      <Modal show={showDetailModal} onHide={() => setShowDetailModal(false)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="h5 fw-bold">Customer KYC Profile</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedCustomer && (
            <Row className="g-3">
              <Col md={6}>
                <div className="text-muted small">Full Name</div>
                <div className="fw-bold fs-6">
                  {selectedCustomer.firstName} {selectedCustomer.lastName}
                </div>
              </Col>
              <Col md={6}>
                <div className="text-muted small">National Identity Card (NIC)</div>
                <div className="fw-bold fs-6">{selectedCustomer.nic}</div>
              </Col>
              <Col md={6}>
                <div className="text-muted small">Email Address</div>
                <div>{selectedCustomer.email}</div>
              </Col>
              <Col md={6}>
                <div className="text-muted small">Phone Number</div>
                <div>{selectedCustomer.phone || 'Not provided'}</div>
              </Col>
              <Col md={6}>
                <div className="text-muted small">Date of Birth</div>
                <div>{formatDate(selectedCustomer.dateOfBirth)}</div>
              </Col>
              <Col md={6}>
                <div className="text-muted small">Account Status</div>
                <div>
                  {selectedCustomer.active ? (
                    <Badge bg="success">Active</Badge>
                  ) : (
                    <Badge bg="secondary">Inactive</Badge>
                  )}
                </div>
              </Col>
              <Col md={12}>
                <div className="text-muted small">Residential Address</div>
                <div className="p-2 bg-light rounded mt-1">
                  {selectedCustomer.addressLine1}
                  {selectedCustomer.addressLine2 && `, ${selectedCustomer.addressLine2}`}
                  {selectedCustomer.city && `, ${selectedCustomer.city}`}
                  {selectedCustomer.postalCode && ` - ${selectedCustomer.postalCode}`}
                </div>
              </Col>
            </Row>
          )}
        </Modal.Body>
        <Modal.Footer>
          {selectedCustomer && (
            <Button
              variant={selectedCustomer.active ? 'outline-danger' : 'outline-success'}
              size="sm"
              onClick={() => handleToggleStatus(selectedCustomer)}
            >
              {selectedCustomer.active ? 'Deactivate Customer' : 'Activate Customer'}
            </Button>
          )}
          <Button variant="secondary" size="sm" onClick={() => setShowDetailModal(false)}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
