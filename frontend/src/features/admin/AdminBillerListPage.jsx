import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  Badge,
  Button,
  Card,
  Col,
  Form,
  Modal,
  Row,
  Spinner,
  Table,
} from 'react-bootstrap';
import { billersApi } from '../../api/billers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatDateTime } from '../../utils/formatDate.js';

export default function AdminBillerListPage() {
  const { showSuccess, showError } = useToast();

  const [billers, setBillers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingBiller, setEditingBiller] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm();

  const fetchBillers = async () => {
    try {
      setLoading(true);
      const res = await billersApi.getAllBillers({ page: 0, size: 50 });
      setBillers(res.data.data?.content || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load billers.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBillers();
  }, []);

  const handleOpenAddModal = () => {
    setEditingBiller(null);
    reset({ code: '', name: '', category: '', referenceFormat: '' });
    setShowModal(true);
  };

  const handleOpenEditModal = (b) => {
    setEditingBiller(b);
    reset({
      code: b.code,
      name: b.name,
      category: b.category,
      referenceFormat: b.referenceFormat || '',
    });
    setShowModal(true);
  };

  const handleToggleStatus = async (b) => {
    const nextStatus = b.status === 'ACTIVE' ? 'RETIRED' : 'ACTIVE';
    try {
      await billersApi.updateBillerStatus(b.id, nextStatus);
      showSuccess(`Biller status changed to ${nextStatus}.`);
      fetchBillers();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to update biller status.');
    }
  };

  const onSubmitForm = async (values) => {
    try {
      setSubmitting(true);
      if (editingBiller) {
        await billersApi.updateBiller(editingBiller.id, values);
        showSuccess('Biller updated successfully.');
      } else {
        await billersApi.createBiller(values);
        showSuccess('Biller created successfully.');
      }
      setShowModal(false);
      fetchBillers();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to save biller.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">Utility & Service Biller Directory</h2>
          <p className="text-muted mb-0">Configure billers available for customer payments</p>
        </div>
        <Button variant="primary" onClick={handleOpenAddModal}>
          <i className="bi bi-plus-circle me-1"></i> Add New Biller
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : billers.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-building fs-1 d-block mb-2"></i>
              No billers configured.
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Reference Format</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {billers.map((b) => (
                    <tr key={b.id}>
                      <td><code>{b.code}</code></td>
                      <td className="fw-semibold">{b.name}</td>
                      <td>{b.category}</td>
                      <td className="text-muted small">{b.referenceFormat || '—'}</td>
                      <td>
                        <Badge bg={b.status === 'ACTIVE' ? 'success' : 'secondary'}>
                          {b.status}
                        </Badge>
                      </td>
                      <td className="text-end">
                        <Button
                          variant="outline-secondary"
                          size="sm"
                          className="me-2"
                          onClick={() => handleOpenEditModal(b)}
                        >
                          <i className="bi bi-pencil me-1"></i> Edit
                        </Button>
                        <Button
                          variant={b.status === 'ACTIVE' ? 'outline-warning' : 'outline-success'}
                          size="sm"
                          onClick={() => handleToggleStatus(b)}
                        >
                          {b.status === 'ACTIVE' ? 'Retire' : 'Activate'}
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

      {/* Add / Edit Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">
            {editingBiller ? 'Edit Biller' : 'Add New Biller'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit(onSubmitForm)}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Biller Code *</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g. CEB, LECO, DIALOG"
                {...register('code', { required: 'Code is required', maxLength: 30 })}
                isInvalid={!!errors.code}
              />
              <Form.Control.Feedback type="invalid">
                {errors.code?.message}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Biller Name *</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g. Ceylon Electricity Board"
                {...register('name', { required: 'Name is required', maxLength: 100 })}
                isInvalid={!!errors.name}
              />
              <Form.Control.Feedback type="invalid">
                {errors.name?.message}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Category *</Form.Label>
              <Form.Select {...register('category', { required: true })}>
                <option value="Electricity">Electricity</option>
                <option value="Water">Water</option>
                <option value="Telecommunications">Telecommunications</option>
                <option value="Insurance">Insurance</option>
                <option value="Education">Education</option>
                <option value="Municipal / Government">Municipal / Government</option>
                <option value="Other">Other</option>
              </Form.Select>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label className="fw-semibold">Reference Format Hint</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g. 10-digit Account Number"
                {...register('referenceFormat', { maxLength: 100 })}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? <Spinner size="sm" className="me-2" /> : null}
              {editingBiller ? 'Save Changes' : 'Create Biller'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
