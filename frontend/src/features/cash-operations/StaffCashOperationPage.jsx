import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  InputGroup,
  ListGroup,
  Modal,
  Row,
  Spinner,
  Table,
} from 'react-bootstrap';
import { cashOperationsApi } from '../../api/cashOperations.api.js';
import { accountApi } from '../../api/accounts.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDateTime } from '../../utils/formatDate.js';

export default function StaffCashOperationPage() {
  const { showSuccess, showError } = useToast();

  const [operations, setOperations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [showCompensateModal, setShowCompensateModal] = useState(false);
  const [compensating, setCompensating] = useState(false);
  const [compensationReason, setCompensationReason] = useState('');
  const [receiptModalData, setReceiptModalData] = useState(null);

  // Account search & selection state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [manualIdEntry, setManualIdEntry] = useState(false);

  // Form for new cash operation
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: {
      accountId: '',
      type: 'DEPOSIT',
      amount: '',
      explanation: '',
    },
  });

  const selectedType = watch('type');
  const enteredAmount = watch('amount');

  const fetchOperations = async () => {
    try {
      setLoading(true);
      const res = await cashOperationsApi.getCashOperations({ page: 0, size: 20 });
      setOperations(res.data.data?.content || []);
    } catch (err) {
      showError(err.apiError?.message || 'Failed to load cash operations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOperations();
  }, []);

  // Search accounts by account number, customer name, email, or NIC
  const handleSearchAccounts = async (e) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      setSearching(true);
      const res = await accountApi.searchAllAccounts({
        search: searchQuery.trim(),
        status: 'ACTIVE',
        size: 10,
      });
      const results = res.data.data?.content || res.data.data || [];
      setSearchResults(results);
      if (results.length === 0) {
        showError('No active accounts found matching: ' + searchQuery);
      }
    } catch (err) {
      showError(err.apiError?.message || 'Error searching accounts.');
    } finally {
      setSearching(false);
    }
  };

  const handleSelectAccount = (acc) => {
    setSelectedAccount(acc);
    setValue('accountId', acc.id, { shouldValidate: true });
    setSearchResults([]);
    setSearchQuery('');
  };

  const handleClearSelectedAccount = () => {
    setSelectedAccount(null);
    setValue('accountId', '', { shouldValidate: true });
  };

  const onSubmitOperation = async (values) => {
    // Validate withdrawal balance
    if (values.type === 'WITHDRAWAL' && selectedAccount) {
      const amountNum = Number(values.amount);
      if (amountNum > Number(selectedAccount.balance)) {
        showError(
          `Insufficient funds: Account balance is ${formatMoney(selectedAccount.balance, selectedAccount.currency)}, requested withdrawal is ${formatMoney(amountNum, selectedAccount.currency)}.`
        );
        return;
      }
    }

    try {
      setSubmitting(true);
      const requestKey = 'CASH-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
      const payload = {
        accountId: Number(values.accountId),
        type: values.type,
        amount: Number(values.amount),
        explanation: values.explanation.trim(),
        requestKey,
      };

      const res = await cashOperationsApi.postCashOperation(payload);
      showSuccess(`Cash ${values.type} completed successfully!`);
      setReceiptModalData(res.data.data);
      reset({ accountId: '', type: 'DEPOSIT', amount: '', explanation: '' });
      setSelectedAccount(null);
      fetchOperations();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to post cash operation.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenCompensate = (op) => {
    setSelectedOperation(op);
    setCompensationReason('');
    setShowCompensateModal(true);
  };

  const handleConfirmCompensation = async () => {
    if (!compensationReason.trim()) {
      showError('Please enter a reason for this compensation.');
      return;
    }
    try {
      setCompensating(true);
      await cashOperationsApi.createCompensation({
        originalLedgerEntryId: selectedOperation.id,
        reason: compensationReason.trim(),
      });
      showSuccess('Compensation reversal posted successfully.');
      setShowCompensateModal(false);
      fetchOperations();
    } catch (err) {
      showError(err.apiError?.message || 'Failed to post compensation reversal.');
    } finally {
      setCompensating(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bold mb-1">Teller Cash Desk Operations</h2>
          <p className="text-muted mb-0">
            Process cash deposits and cash withdrawals on customer accounts with instant receipting.
          </p>
        </div>
        <Button variant="outline-secondary" onClick={fetchOperations} disabled={loading}>
          <i className="bi bi-arrow-clockwise me-1"></i> Refresh
        </Button>
      </div>

      <Row className="g-4 mb-4">
        {/* Post Operation Form */}
        <Col lg={5}>
          <Card className="shadow-sm border-0">
            <Card.Header className="bg-light fw-bold d-flex justify-content-between align-items-center">
              <span>
                <i className="bi bi-cash-stack me-2 text-primary"></i>New Cash Transaction
              </span>
              <Button
                variant="link"
                size="sm"
                className="p-0 text-decoration-none small text-muted"
                onClick={() => setManualIdEntry(!manualIdEntry)}
              >
                {manualIdEntry ? 'Search by details' : 'Enter ID directly'}
              </Button>
            </Card.Header>
            <Card.Body>
              {/* Account Search Interface */}
              {!manualIdEntry && (
                <div className="mb-3">
                  <Form.Label className="fw-semibold">Find Customer Account</Form.Label>
                  {!selectedAccount ? (
                    <div>
                      <InputGroup>
                        <Form.Control
                          type="text"
                          placeholder="Search account number, name, NIC, or email..."
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          onKeyDown={(e) => e.key === 'Enter' && handleSearchAccounts(e)}
                        />
                        <Button
                          variant="primary"
                          type="button"
                          onClick={handleSearchAccounts}
                          disabled={searching || !searchQuery.trim()}
                        >
                          {searching ? <Spinner size="sm" /> : <i className="bi bi-search me-1" />}
                          Search
                        </Button>
                      </InputGroup>
                      <Form.Text className="text-muted small">
                        Lookup active accounts by full name, NIC, account number, or email.
                      </Form.Text>

                      {/* Search Results Dropdown/List */}
                      {searchResults.length > 0 && (
                        <ListGroup className="mt-2 shadow-sm border rounded">
                          {searchResults.map((acc) => (
                            <ListGroup.Item
                              key={acc.id}
                              action
                              onClick={() => handleSelectAccount(acc)}
                              className="d-flex justify-content-between align-items-center p-2"
                            >
                              <div>
                                <div className="fw-bold small">{acc.customerName}</div>
                                <div className="text-muted small">
                                  <code>{acc.accountNumber}</code> ({acc.type})
                                  {acc.customerNic && ` • NIC: ${acc.customerNic}`}
                                </div>
                              </div>
                              <div className="text-end">
                                <span className="fw-semibold text-success small d-block">
                                  {formatMoney(acc.balance, acc.currency)}
                                </span>
                                <Badge bg="primary" pill style={{ fontSize: '0.7rem' }}>
                                  Select
                                </Badge>
                              </div>
                            </ListGroup.Item>
                          ))}
                        </ListGroup>
                      )}
                    </div>
                  ) : (
                    /* Selected Account Card Preview */
                    <Card className="border border-primary bg-light">
                      <Card.Body className="p-3">
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <div>
                            <div className="fw-bold fs-6 text-dark">{selectedAccount.customerName}</div>
                            <div className="text-muted small">
                              <code>{selectedAccount.accountNumber}</code>
                              <Badge bg="secondary" className="ms-2">
                                {selectedAccount.type}
                              </Badge>
                            </div>
                          </div>
                          <Button
                            variant="outline-danger"
                            size="sm"
                            className="py-0 px-2"
                            onClick={handleClearSelectedAccount}
                            title="Choose different account"
                          >
                            Change
                          </Button>
                        </div>

                        <div className="d-flex justify-content-between align-items-center pt-2 border-top">
                          <span className="text-muted small">Available Balance:</span>
                          <span className="fw-bold fs-6 text-primary">
                            {formatMoney(selectedAccount.balance, selectedAccount.currency)}
                          </span>
                        </div>
                        {selectedAccount.customerNic && (
                          <div className="small text-muted mt-1">
                            NIC: <strong>{selectedAccount.customerNic}</strong>
                            {selectedAccount.customerEmail && ` • ${selectedAccount.customerEmail}`}
                          </div>
                        )}
                      </Card.Body>
                    </Card>
                  )}
                </div>
              )}

              <Form onSubmit={handleSubmit(onSubmitOperation)}>
                {/* Manual Account ID Field (only shown if toggled or selected via search) */}
                {manualIdEntry && (
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">Account ID</Form.Label>
                    <Form.Control
                      type="number"
                      placeholder="e.g. 1"
                      {...register('accountId', {
                        required: 'Account ID is required',
                        min: { value: 1, message: 'Invalid Account ID' },
                      })}
                      isInvalid={!!errors.accountId}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.accountId?.message}
                    </Form.Control.Feedback>
                  </Form.Group>
                )}

                {/* Hidden Account ID to satisfy form validation when using search */}
                {!manualIdEntry && (
                  <input
                    type="hidden"
                    {...register('accountId', {
                      required: 'Please search and select a customer account',
                    })}
                  />
                )}
                {!manualIdEntry && errors.accountId && (
                  <div className="text-danger small mb-3">
                    <i className="bi bi-exclamation-circle me-1" />
                    {errors.accountId.message}
                  </div>
                )}

                {/* Operation Type */}
                <Form.Group className="mb-3">
                  <Form.Label className="fw-semibold">Operation Type</Form.Label>
                  <Form.Select {...register('type', { required: true })}>
                    <option value="DEPOSIT">Cash Deposit (Credit account)</option>
                    <option value="WITHDRAWAL">Cash Withdrawal (Debit account)</option>
                  </Form.Select>
                </Form.Group>

                {/* Amount */}
                <Form.Group className="mb-3">
                  <Form.Label className="fw-semibold">Amount (LKR)</Form.Label>
                  <InputGroup>
                    <InputGroup.Text>LKR</InputGroup.Text>
                    <Form.Control
                      type="number"
                      step="0.01"
                      placeholder="0.00"
                      {...register('amount', {
                        required: 'Amount is required',
                        min: { value: 1, message: 'Minimum amount is LKR 1.00' },
                        max: { value: 10000000, message: 'Exceeds maximum single transaction limit' },
                      })}
                      isInvalid={!!errors.amount}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.amount?.message}
                    </Form.Control.Feedback>
                  </InputGroup>
                  {selectedType === 'WITHDRAWAL' && selectedAccount && (
                    <Form.Text className="text-muted small">
                      Max available for withdrawal:{' '}
                      <strong>{formatMoney(selectedAccount.balance, selectedAccount.currency)}</strong>
                    </Form.Text>
                  )}
                </Form.Group>

                {/* Explanation */}
                <Form.Group className="mb-4">
                  <Form.Label className="fw-semibold">Teller Remarks / Explanation</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={2}
                    placeholder="e.g. Over-the-counter cash deposit by customer"
                    {...register('explanation', {
                      required: 'Explanation is required for teller records',
                      minLength: { value: 3, message: 'Please provide at least 3 characters' },
                    })}
                    isInvalid={!!errors.explanation}
                  />
                  <Form.Control.Feedback type="invalid">
                    {errors.explanation?.message}
                  </Form.Control.Feedback>
                </Form.Group>

                <Button
                  variant="primary"
                  type="submit"
                  className="w-100 fw-bold py-2"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <Spinner size="sm" className="me-2" />
                      Processing...
                    </>
                  ) : (
                    `Submit Cash ${selectedType === 'DEPOSIT' ? 'Deposit' : 'Withdrawal'}`
                  )}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>

        {/* History Table */}
        <Col lg={7}>
          <Card className="shadow-sm border-0">
            <Card.Header className="bg-light fw-bold">
              <i className="bi bi-clock-history me-2 text-secondary"></i>Recent Cash Desk Operations
            </Card.Header>
            <Card.Body className="p-0">
              {loading ? (
                <div className="text-center py-5">
                  <Spinner animation="border" variant="primary" />
                </div>
              ) : operations.length === 0 ? (
                <div className="text-center py-5 text-muted">
                  <i className="bi bi-inbox fs-1 d-block mb-2"></i>
                  No cash operations recorded today.
                </div>
              ) : (
                <div className="table-responsive">
                  <Table hover className="align-middle mb-0">
                    <thead className="table-light">
                      <tr>
                        <th>Date & Ref</th>
                        <th>Account</th>
                        <th>Type</th>
                        <th className="text-end">Amount</th>
                        <th className="text-center">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {operations.map((op) => (
                        <tr key={op.id}>
                          <td>
                            <div className="fw-semibold text-truncate">{op.reference}</div>
                            <div className="text-muted small">{formatDateTime(op.createdAt)}</div>
                          </td>
                          <td>
                            <div className="fw-semibold">{op.customerName || 'N/A'}</div>
                            <div className="text-muted small"><code>{op.accountNumber}</code></div>
                          </td>
                          <td>
                            <Badge bg={op.type === 'DEPOSIT' ? 'success' : 'warning'} className="text-uppercase">
                              {op.type}
                            </Badge>
                          </td>
                          <td className="text-end fw-bold">
                            <span className={op.type === 'DEPOSIT' ? 'text-success' : 'text-danger'}>
                              {op.type === 'DEPOSIT' ? '+' : '-'} {formatMoney(op.amount)}
                            </span>
                          </td>
                          <td className="text-center">
                            <Button
                              variant="outline-danger"
                              size="sm"
                              title="Reverse with compensation"
                              onClick={() => handleOpenCompensate(op)}
                            >
                              <i className="bi bi-arrow-counterclockwise"></i> Reverse
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
        </Col>
      </Row>

      {/* Receipt Modal */}
      <Modal show={!!receiptModalData} onHide={() => setReceiptModalData(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold">
            <i className="bi bi-check-circle-fill text-success me-2"></i>Transaction Receipt
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {receiptModalData && (
            <div>
              <div className="alert alert-success text-center mb-3">
                <h4 className="fw-bold mb-0">
                  {receiptModalData.type === 'DEPOSIT' ? 'Deposit Successful' : 'Withdrawal Successful'}
                </h4>
                <div className="small text-muted">{receiptModalData.reference}</div>
              </div>
              <Table borderless size="sm">
                <tbody>
                  <tr>
                    <td className="text-muted">Account Number:</td>
                    <td className="fw-bold text-end"><code>{receiptModalData.accountNumber}</code></td>
                  </tr>
                  <tr>
                    <td className="text-muted">Account Holder:</td>
                    <td className="fw-bold text-end">{receiptModalData.customerName}</td>
                  </tr>
                  <tr>
                    <td className="text-muted">Transaction Amount:</td>
                    <td className="fw-bold text-end">{formatMoney(receiptModalData.amount)}</td>
                  </tr>
                  <tr>
                    <td className="text-muted">Updated Balance:</td>
                    <td className="fw-bold text-end text-primary">{formatMoney(receiptModalData.balanceAfter)}</td>
                  </tr>
                  <tr>
                    <td className="text-muted">Timestamp:</td>
                    <td className="text-end">{formatDateTime(receiptModalData.createdAt)}</td>
                  </tr>
                </tbody>
              </Table>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setReceiptModalData(null)}>
            Close Receipt
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Compensation Reversal Modal */}
      <Modal show={showCompensateModal} onHide={() => setShowCompensateModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fw-bold text-danger">
            <i className="bi bi-exclamation-triangle-fill me-2"></i>Compensate Transaction
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedOperation && (
            <div>
              <p className="text-muted small">
                Financial records in the system are strictly immutable. This action will append a
                counter-balancing reversing entry to restore account balances.
              </p>
              <div className="p-3 bg-light rounded mb-3">
                <div><strong>Reference:</strong> <code>{selectedOperation.reference}</code></div>
                <div><strong>Account:</strong> {selectedOperation.accountNumber} ({selectedOperation.customerName})</div>
                <div><strong>Amount:</strong> {formatMoney(selectedOperation.amount)} ({selectedOperation.type})</div>
              </div>
              <Form.Group>
                <Form.Label className="fw-semibold">Reason for Compensation Reversal *</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  placeholder="e.g. Erroneous cash deposit entry entered by teller"
                  value={compensationReason}
                  onChange={(e) => setCompensationReason(e.target.value)}
                />
              </Form.Group>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowCompensateModal(false)} disabled={compensating}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleConfirmCompensation} disabled={compensating}>
            {compensating ? <Spinner size="sm" className="me-2" /> : null}
            Confirm Reversal
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
