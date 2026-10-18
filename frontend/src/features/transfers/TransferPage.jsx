import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Form, ProgressBar, Row, Spinner, Table } from 'react-bootstrap';
import { accountApi } from '../../api/accounts.api.js';
import { beneficiaryApi } from '../../api/beneficiaries.api.js';
import { transferApi } from '../../api/transfers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDateTime } from '../../utils/formatDate.js';

/**
 * Fund Transfer Component.
 *
 * Implements a high-security, 3-step wizard workflow for inter-account fund transfers:
 *
 * Step 1: Transfer Details Entry
 * - Source account selection from active customer accounts
 * - Quick recipient auto-fill from saved beneficiaries or manual entry
 * - Client-side amount validation and real-time available balance display
 *
 * Step 2: Review & Confirmation (Two-Phase Commitment)
 * - Invokes backend `POST /api/transfers/preview` to dry-run validation without mutating ledger
 * - Displays the verified destination account holder's full legal name (preventing wrong-account transfers)
 * - Generates a cryptographic idempotency key (`crypto.randomUUID()`) to protect against duplicate submissions
 *
 * Step 3: Execution & Immutable Receipt
 * - Invokes backend `POST /api/transfers` with the idempotency key and confirmed payload
 * - Backend executes atomic row-level locked double-entry bookkeeping (Debit Source, Credit Destination)
 * - Renders a digital transaction receipt containing the permanent audit reference and timestamp
 */
export default function TransferPage() {
  const { showError, showSuccess } = useToast();

  // Wizard state: 1 = Input Details, 2 = Review & Confirm, 3 = Receipt View
  const [step, setStep] = useState(1);

  // Master data
  const [accounts, setAccounts] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Transaction preview, execution result, and idempotency key
  const [previewData, setPreviewData] = useState(null);
  const [resultData, setResultData] = useState(null);
  const [requestKey, setRequestKey] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  // React Hook Form for form state management and input validation
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      sourceAccountId: '',
      destinationAccountNumber: '',
      amount: '',
      remarks: '',
    },
  });

  // Watch selected source account to dynamically display available balance
  const selectedSourceId = watch('sourceAccountId');
  const selectedSourceAccount = accounts.find((a) => a.id === Number(selectedSourceId));

  /**
   * Initializes transfer form by fetching customer's active bank accounts and saved beneficiaries in parallel.
   */
  useEffect(() => {
    const loadData = async () => {
      try {
        setLoadingInitial(true);
        const [accRes, benRes] = await Promise.all([
          accountApi.getMyAccounts(),
          beneficiaryApi.getMyBeneficiaries(),
        ]);
        // Only ACTIVE accounts can initiate outgoing transfers
        const activeAccs = (accRes.data.data || []).filter((a) => a.status === 'ACTIVE');
        setAccounts(activeAccs);
        setBeneficiaries(benRes.data.data || []);
        if (activeAccs.length > 0) {
          setValue('sourceAccountId', activeAccs[0].id);
        }
      } catch (err) {
        showError(err.apiError?.message || 'Failed to initialize transfer form.');
      } finally {
        setLoadingInitial(false);
      }
    };
    loadData();
  }, [setValue]);

  /**
   * Step 1 -> Step 2: Validates transfer details with backend preview API.
   *
   * Dry-runs the transfer to verify:
   * 1. Destination account exists and is ACTIVE.
   * 2. Source account has sufficient funds.
   * 3. Source and destination are not identical.
   * Generates a unique UUID `requestKey` to ensure idempotent execution in Step 2.
   */
  const handlePreview = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      const res = await transferApi.previewTransfer({
        sourceAccountId: Number(data.sourceAccountId),
        destinationAccountNumber: data.destinationAccountNumber.trim(),
        amount: parseFloat(data.amount),
      });

      setPreviewData({
        ...res.data.data,
        remarks: data.remarks,
      });

      // Generate UUID requestKey for duplicate submission prevention
      setRequestKey(crypto.randomUUID());
      setStep(2);
    } catch (err) {
      const msg = err.apiError?.message || 'Validation failed. Please check the transfer details.';
      setServerError(msg);
      showError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Step 2 -> Step 3: Executes the atomic fund transfer.
   *
   * Posts transfer request with the pre-generated `requestKey`.
   * On success, transitions wizard to the printable receipt step.
   */
  const handleConfirmTransfer = async () => {
    setServerError('');
    setSubmitting(true);
    try {
      const res = await transferApi.postTransfer({
        requestKey: requestKey,
        sourceAccountId: previewData.sourceAccountId,
        destinationAccountNumber: previewData.destinationAccountNumber,
        amount: previewData.amount,
        remarks: previewData.remarks,
      });

      setResultData(res.data.data);
      showSuccess('Funds transferred successfully!');
      setStep(3);
    } catch (err) {
      const msg = err.apiError?.message || 'Transfer failed. Your balance remains unchanged.';
      setServerError(msg);
      showError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Resets form state and starts a fresh transfer flow.
   */
  const handleStartNewTransfer = () => {
    reset({
      sourceAccountId: accounts[0]?.id || '',
      destinationAccountNumber: '',
      amount: '',
      remarks: '',
    });
    setPreviewData(null);
    setResultData(null);
    setRequestKey('');
    setStep(1);
  };

  if (loadingInitial) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" role="status" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '680px' }} className="mx-auto">
      <div className="mb-4 pb-2 border-bottom">
        <h1 className="h3 mb-1">Fund Transfer</h1>
        <p className="text-muted mb-0 small">Transfer funds securely between bank accounts</p>
      </div>

      {/* 3-Step Wizard Progress Bar */}
      <div className="mb-4">
        <ProgressBar now={step === 1 ? 33 : step === 2 ? 66 : 100} variant="primary" style={{ height: '6px' }} />
        <div className="d-flex justify-content-between small text-muted mt-2">
          <span className={step >= 1 ? 'fw-bold text-primary' : ''}>1. Transfer Details</span>
          <span className={step >= 2 ? 'fw-bold text-primary' : ''}>2. Review & Confirm</span>
          <span className={step === 3 ? 'fw-bold text-primary' : ''}>3. Receipt</span>
        </div>
      </div>

      {serverError && (
        <Alert variant="danger" className="py-2 small mb-4">
          <i className="bi bi-exclamation-triangle-fill me-2" />
          {serverError}
        </Alert>
      )}

      {/* =========================================================================
          STEP 1: TRANSFER DETAILS ENTRY FORM
          ========================================================================= */}
      {step === 1 && (
        <Card className="shadow-sm border-0">
          <Card.Header className="bg-white py-3">
            <h5 className="mb-0 fw-semibold">Enter Transfer Details</h5>
          </Card.Header>
          <Card.Body className="p-4">
            <Form onSubmit={handleSubmit(handlePreview)}>
              {/* Source Account Selection */}
              <Form.Group className="mb-3" controlId="source-account-select">
                <Form.Label className="fw-semibold">From Account</Form.Label>
                <Form.Select
                  isInvalid={!!errors.sourceAccountId}
                  {...register('sourceAccountId', { required: 'Please select a source account' })}
                >
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountNumber} ({a.type}) — Available: {formatMoney(a.balance)}
                    </option>
                  ))}
                </Form.Select>
                {selectedSourceAccount && (
                  <Form.Text className="text-muted">
                    Available balance:{' '}
                    <strong className="text-dark">{formatMoney(selectedSourceAccount.balance)}</strong>
                  </Form.Text>
                )}
                <Form.Control.Feedback type="invalid">{errors.sourceAccountId?.message}</Form.Control.Feedback>
              </Form.Group>

              {/* Quick Beneficiary Selector */}
              {beneficiaries.length > 0 && (
                <Form.Group className="mb-3">
                  <Form.Label className="small text-muted">Or select from saved beneficiaries:</Form.Label>
                  <Form.Select
                    onChange={(e) => {
                      if (e.target.value) {
                        setValue('destinationAccountNumber', e.target.value);
                      }
                    }}
                    defaultValue=""
                  >
                    <option value="">-- Choose a saved beneficiary --</option>
                    {beneficiaries.map((b) => (
                      <option key={b.id} value={b.accountNumber}>
                        {b.name} ({b.accountNumber}) {b.nickname ? `[${b.nickname}]` : ''}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              )}

              {/* Destination Account Number */}
              <Form.Group className="mb-3" controlId="dest-account-input">
                <Form.Label className="fw-semibold">Recipient Account Number</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="12-digit account number"
                  isInvalid={!!errors.destinationAccountNumber}
                  {...register('destinationAccountNumber', {
                    required: 'Destination account number is required',
                    minLength: { value: 10, message: 'Account number must be at least 10 characters' },
                    maxLength: { value: 12, message: 'Account number must not exceed 12 characters' },
                  })}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.destinationAccountNumber?.message}
                </Form.Control.Feedback>
              </Form.Group>

              {/* Amount */}
              <Form.Group className="mb-3" controlId="amount-input">
                <Form.Label className="fw-semibold">Amount (LKR)</Form.Label>
                <Form.Control
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="0.00"
                  isInvalid={!!errors.amount}
                  {...register('amount', {
                    required: 'Amount is required',
                    min: { value: 0.01, message: 'Amount must be at least LKR 0.01' },
                  })}
                />
                <Form.Control.Feedback type="invalid">{errors.amount?.message}</Form.Control.Feedback>
              </Form.Group>

              {/* Remarks */}
              <Form.Group className="mb-4" controlId="remarks-input">
                <Form.Label>Remarks / Reference (Optional)</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="e.g. Rent, Grocery repayment"
                  maxLength={200}
                  {...register('remarks')}
                />
              </Form.Group>

              <div className="d-flex justify-content-end">
                <Button variant="primary" type="submit" disabled={submitting}>
                  {submitting ? (
                    <>
                      <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                      Validating...
                    </>
                  ) : (
                    'Proceed to Review'
                  )}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}

      {/* =========================================================================
          STEP 2: REVIEW & CONFIRMATION SCREEN
          ========================================================================= */}
      {step === 2 && previewData && (
        <Card className="shadow-sm border-0">
          <Card.Header className="bg-white py-3">
            <h5 className="mb-0 fw-semibold">Review Transfer Details</h5>
          </Card.Header>
          <Card.Body className="p-4">
            <Alert variant="info" className="py-2 small mb-4">
              <i className="bi bi-info-circle-fill me-2" />
              Please verify the recipient name and amount before confirming the transaction.
            </Alert>

            <Table borderless className="mb-4">
              <tbody>
                <tr>
                  <td className="text-muted" style={{ width: '40%' }}>From Account:</td>
                  <td className="fw-semibold font-monospace">{previewData.sourceAccountNumber}</td>
                </tr>
                <tr>
                  <td className="text-muted">Recipient Name:</td>
                  <td className="fw-bold text-primary">{previewData.recipientName}</td>
                </tr>
                <tr>
                  <td className="text-muted">Recipient Account:</td>
                  <td className="fw-semibold font-monospace">{previewData.destinationAccountNumber}</td>
                </tr>
                <tr>
                  <td className="text-muted">Transfer Amount:</td>
                  <td className="h4 fw-bold text-success mb-0">{formatMoney(previewData.amount)}</td>
                </tr>
                {previewData.remarks && (
                  <tr>
                    <td className="text-muted">Remarks:</td>
                    <td>{previewData.remarks}</td>
                  </tr>
                )}
              </tbody>
            </Table>

            <div className="d-flex justify-content-between">
              <Button variant="outline-secondary" onClick={() => setStep(1)} disabled={submitting}>
                Back to Edit
              </Button>
              <Button variant="success" onClick={handleConfirmTransfer} disabled={submitting}>
                {submitting ? (
                  <>
                    <Spinner as="span" animation="border" size="sm" role="status" className="me-2" />
                    Processing Transfer...
                  </>
                ) : (
                  'Confirm & Transfer'
                )}
              </Button>
            </div>
          </Card.Body>
        </Card>
      )}

      {/* =========================================================================
          STEP 3: TRANSACTION SUCCESS RECEIPT
          ========================================================================= */}
      {step === 3 && resultData && (
        <Card className="shadow-sm border-0">
          <Card.Body className="p-5 text-center">
            <i className="bi bi-check-circle-fill text-success display-3 d-block mb-3" />
            <h2 className="h4 fw-bold mb-1">Transfer Completed Successfully</h2>
            <p className="text-muted small mb-4">The funds have been transferred immediately to the recipient</p>

            <Card className="bg-light border-0 mb-4 text-start">
              <Card.Body className="p-4">
                <Row className="mb-2">
                  <Col sm={5} className="text-muted">Transaction Reference:</Col>
                  <Col sm={7} className="fw-bold font-monospace">{resultData.reference}</Col>
                </Row>
                <Row className="mb-2">
                  <Col sm={5} className="text-muted">Date & Time:</Col>
                  <Col sm={7}>{formatDateTime(resultData.timestamp)}</Col>
                </Row>
                <Row className="mb-2">
                  <Col sm={5} className="text-muted">From Account:</Col>
                  <Col sm={7} className="font-monospace">{resultData.sourceAccountNumber}</Col>
                </Row>
                <Row className="mb-2">
                  <Col sm={5} className="text-muted">To Account:</Col>
                  <Col sm={7} className="font-monospace">{resultData.destinationAccountNumber}</Col>
                </Row>
                <Row className="mb-2">
                  <Col sm={5} className="text-muted">Recipient Name:</Col>
                  <Col sm={7} className="fw-semibold">{resultData.recipientName}</Col>
                </Row>
                <Row className="mb-0">
                  <Col sm={5} className="text-muted">Amount Transferred:</Col>
                  <Col sm={7} className="h5 fw-bold text-success mb-0">{formatMoney(resultData.amount)}</Col>
                </Row>
              </Card.Body>
            </Card>

            <div className="d-flex justify-content-center gap-3">
              <Button as={Link} to={PATHS.DASHBOARD} variant="outline-primary">
                Back to Dashboard
              </Button>
              <Button variant="primary" onClick={handleStartNewTransfer}>
                Make Another Transfer
              </Button>
            </div>
          </Card.Body>
        </Card>
      )}
    </div>
  );
}
