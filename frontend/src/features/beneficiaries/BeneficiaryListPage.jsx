import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button, Card, Form, Modal, Spinner, Table } from 'react-bootstrap';
import { beneficiaryApi } from '../../api/beneficiaries.api.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function BeneficiaryListPage() {
  const { showSuccess, showError } = useToast();

  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingBeneficiary, setEditingBeneficiary] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm();

  const fetchBeneficiaries = async () => {
    try {
      setLoading(true);
      const res = await beneficiaryApi.getMyBeneficiaries();
      setBeneficiaries(res.data.data || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load beneficiaries.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBeneficiaries();
  }, []);

  const handleOpenAddModal = () => {
    setEditingBeneficiary(null);
    reset({ name: '', accountNumber: '', nickname: '' });
    setShowModal(true);
  };

  const handleOpenEditModal = (b) => {
    setEditingBeneficiary(b);
    reset({ name: b.name, accountNumber: b.accountNumber, nickname: b.nickname || '' });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this beneficiary?')) {
      return;
    }
    try {
      await beneficiaryApi.deleteBeneficiary(id);
      showSuccess('Beneficiary removed.');
      fetchBeneficiaries();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to remove beneficiary.');
    }
  };

  const onSubmit = async (data) => {
    setSubmitting(true);
    try {
      if (editingBeneficiary) {
        await beneficiaryApi.updateBeneficiary(editingBeneficiary.id, data);
        showSuccess('Beneficiary updated.');
      } else {
        await beneficiaryApi.addBeneficiary(data);
        showSuccess('Beneficiary added successfully.');
      }
      setShowModal(false);
      fetchBeneficiaries();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to save beneficiary.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h1 className="h3 mb-1">Beneficiaries</h1>
          <p className="text-muted mb-0 small">Save and manage recipient accounts for quick fund transfers</p>
        </div>
        <Button variant="primary" size="sm" onClick={handleOpenAddModal}>
          <i className="bi bi-person-plus me-1" /> Add Beneficiary
        </Button>
      </div>

      <Card className="shadow-sm border-0">
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" role="status" />
            </div>
          ) : beneficiaries.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-people display-4 d-block mb-3 text-secondary" />
              <h5>No beneficiaries saved</h5>
              <p className="mb-3">Add accounts you frequently transfer money to for easy one-click transfers.</p>
              <Button variant="outline-primary" size="sm" onClick={handleOpenAddModal}>
                Add Your First Beneficiary
              </Button>
            </div>
          ) : (
            <Table responsive hover className="align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Recipient Name</th>
                  <th>Account Number</th>
                  <th>Nickname</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {beneficiaries.map((b) => (
                  <tr key={b.id}>
                    <td className="fw-semibold">{b.name}</td>
                    <td className="font-monospace">{b.accountNumber}</td>
                    <td>{b.nickname ? <span className="badge bg-light text-dark">{b.nickname}</span> : '—'}</td>
                    <td className="text-end">
                      <Button variant="outline-secondary" size="sm" className="me-2" onClick={() => handleOpenEditModal(b)}>
                        <i className="bi bi-pencil me-1" /> Edit
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleDelete(b.id)}>
                        <i className="bi bi-trash" />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      {/* Add / Edit Beneficiary Modal */}
      <Modal show={showModal} onHide={() => !submitting && setShowModal(false)} centered>
        <Form onSubmit={handleSubmit(onSubmit)}>
          <Modal.Header closeButton>
            <Modal.Title className="h5">{editingBeneficiary ? 'Edit Beneficiary' : 'Add Beneficiary'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3" controlId="b-name">
              <Form.Label>Recipient Name</Form.Label>
              <Form.Control
                type="text"
                placeholder="Full Name"
                isInvalid={!!errors.name}
                {...register('name', { required: 'Name is required' })}
              />
              <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3" controlId="b-accountNumber">
              <Form.Label>Account Number</Form.Label>
              <Form.Control
                type="text"
                placeholder="12-digit account number"
                isInvalid={!!errors.accountNumber}
                {...register('accountNumber', {
                  required: 'Account number is required',
                  minLength: { value: 10, message: 'Must be at least 10 characters' },
                  maxLength: { value: 12, message: 'Must not exceed 12 characters' },
                })}
              />
              <Form.Control.Feedback type="invalid">{errors.accountNumber?.message}</Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3" controlId="b-nickname">
              <Form.Label>Nickname (Optional)</Form.Label>
              <Form.Control type="text" placeholder="e.g. Landlord, Electricity Fund" {...register('nickname')} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Beneficiary'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
