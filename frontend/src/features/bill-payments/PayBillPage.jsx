import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  ProgressBar,
  Row,
  Spinner,
  Table,
} from 'react-bootstrap';
import { accountApi } from '../../api/accounts.api.js';
import { billersApi } from '../../api/billers.api.js';
import { useToast } from '../../context/ToastContext.jsx';
import PATHS from '../../routes/routePaths.js';
import { formatMoney } from '../../utils/formatMoney.js';
import { formatDateTime } from '../../utils/formatDate.js';

export default function PayBillPage() {
  const { showError, showSuccess } = useToast();

  // Wizard Steps: 1: Enter Details, 2: Review & Confirm, 3: Success Receipt
  const [step, setStep] = useState(1);

  const [accounts, setAccounts] = useState([]);
  const [billers, setBillers] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);

  const [selectedBiller, setSelectedBiller] = useState(null);
  const [reviewData, setReviewData] = useState(null);
  const [receiptData, setReceiptData] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [requestKey, setRequestKey] = useState('');

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
      billerId: '',
      subscriberReference: '',
      amount: '',
      remarks: '',
    },
  });

  const selectedSourceId = watch('sourceAccountId');
  const selectedBillerId = watch('billerId');
  const selectedAccount = accounts.find((a) => a.id === Number(selectedSourceId));

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoadingInitial(true);
        const [accRes, billerRes] = await Promise.all([
          accountApi.getMyAccounts(),
          billersApi.getActiveBillers(),
        ]);
        const activeAccs = (accRes.data.data || []).filter((a) => a.status === 'ACTIVE');
        setAccounts(activeAccs);
        const activeBillers = billerRes.data.data || [];
        setBillers(activeBillers);

        if (activeAccs.length > 0) {
          setValue('sourceAccountId', activeAccs[0].id);
        }
        if (activeBillers.length > 0) {
          setValue('billerId', activeBillers[0].id);
          setSelectedBiller(activeBillers[0]);
        }
      } catch (err) {
        showError(err.apiError?.message || 'Failed to load accounts or billers.');
      } finally {
        setLoadingInitial(false);
      }
    };

    loadData();
  }, [setValue]);

  const handleBillerChange = (e) => {
    const bId = Number(e.target.value);
    setValue('billerId', bId);
    const b = billers.find((item) => item.id === bId);
    setSelectedBiller(b || null);
  };

  // Step 1 -> Step 2 Review
  const onProceedToReview = (data) => {
    const acc = accounts.find((a) => a.id === Number(data.sourceAccountId));
    const bil = billers.find((b) => b.id === Number(data.billerId));
    const amt = parseFloat(data.amount);

    if (!acc) {
      showError('Please select a valid payment account.');
      return;
    }
    if (acc.balance < amt) {
      showError(`Insufficient funds. Your available balance is ${formatMoney(acc.balance, acc.currency)}`);
      return;
    }

    const key = 'BP-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
    setRequestKey(key);
    setReviewData({
      ...data,
      amountNum: amt,
      account: acc,
      biller: bil,
    });
    setStep(2);
  };

  // Step 2 Confirm -> Step 3 Receipt
  const handleConfirmPayment = async () => {
    try {
      setSubmitting(true);
      const payload = {
        requestKey,
        sourceAccountId: Number(reviewData.sourceAccountId),
        billerId: Number(reviewData.billerId),
        subscriberReference: reviewData.subscriberReference.trim(),
        amount: reviewData.amountNum,
        remarks: reviewData.remarks ? reviewData.remarks.trim() : null,
      };

      const res = await billersApi.payBill(payload);
      setReceiptData(res.data.data);
      showSuccess('Bill payment executed successfully!');
      setStep(3);
    } catch (err) {
      showError(err.apiError?.message || 'Bill payment failed. Please check your balance.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStartNewPayment = () => {
    reset({
      sourceAccountId: accounts.length > 0 ? accounts[0].id : '',
      billerId: billers.length > 0 ? billers[0].id : '',
      subscriberReference: '',
      amount: '',
      remarks: '',
    });
    setSelectedBiller(billers[0] || null);
    setReviewData(null);
    setReceiptData(null);
    setStep(1);
  };

  if (loadingInitial) {
    return (
      <div className="container py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="text-muted mt-2">Loading billing services...</p>
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Page Header */}
      <div className="mb-4">
        <h2 className="fw-bold mb-1">Pay Utility & Service Bills</h2>
        <p className="text-muted mb-0">
          Instant settlement for electricity, water, telecom, and other registered utility billers.
        </p>
      </div>

      {/* Progress Bar */}
      <Row className="justify-content-center mb-4">
        <Col md={8}>
          <div className="d-flex justify-content-between small text-muted mb-1">
            <span className={step >= 1 ? 'fw-bold text-primary' : ''}>1. Enter Details</span>
            <span className={step >= 2 ? 'fw-bold text-primary' : ''}>2. Review & Confirm</span>
            <span className={step === 3 ? 'fw-bold text-success' : ''}>3. Receipt</span>
          </div>
          <ProgressBar
            now={step === 1 ? 33 : step === 2 ? 66 : 100}
            variant={step === 3 ? 'success' : 'primary'}
            style={{ height: '6px' }}
          />
        </Col>
      </Row>

      <Row className="justify-content-center">
        <Col md={8} lg={7}>
          {/* STEP 1: Details */}
          {step === 1 && (
            <Card className="shadow-sm border-0">
              <Card.Header className="bg-light fw-bold py-3">
                <i className="bi bi-wallet-fill me-2 text-primary"></i>Payment Details
              </Card.Header>
              <Card.Body className="p-4">
                {accounts.length === 0 ? (
                  <Alert variant="warning">
                    You do not have any ACTIVE bank accounts to make payments from.
                  </Alert>
                ) : (
                  <Form onSubmit={handleSubmit(onProceedToReview)}>
                    {/* Source Account */}
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">Pay From Account *</Form.Label>
                      <Form.Select
                        {...register('sourceAccountId', { required: 'Please select an account' })}
                        isInvalid={!!errors.sourceAccountId}
                      >
                        {accounts.map((acc) => (
                          <option key={acc.id} value={acc.id}>
                            {acc.accountNumber} ({acc.type}) — Balance: {formatMoney(acc.balance, acc.currency)}
                          </option>
                        ))}
                      </Form.Select>
                      {selectedAccount && (
                        <div className="form-text text-muted">
                          Available: <strong>{formatMoney(selectedAccount.balance, selectedAccount.currency)}</strong>
                        </div>
                      )}
                    </Form.Group>

                    {/* Biller Selection */}
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">Select Biller *</Form.Label>
                      <Form.Select
                        value={selectedBillerId}
                        onChange={handleBillerChange}
                        isInvalid={!!errors.billerId}
                      >
                        {billers.map((b) => (
                          <option key={b.id} value={b.id}>
                            {b.name} ({b.category})
                          </option>
                        ))}
                      </Form.Select>
                      {selectedBiller && selectedBiller.referenceFormat && (
                        <div className="form-text text-muted">
                          Expected format: {selectedBiller.referenceFormat}
                        </div>
                      )}
                    </Form.Group>

                    {/* Subscriber Account / Ref */}
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">
                        Subscriber / Account Number *
                      </Form.Label>
                      <Form.Control
                        type="text"
                        placeholder="e.g. 0112345678 or 10-digit utility account"
                        {...register('subscriberReference', {
                          required: 'Subscriber account number is required',
                          minLength: { value: 4, message: 'Must be at least 4 characters' },
                        })}
                        isInvalid={!!errors.subscriberReference}
                      />
                      <Form.Control.Feedback type="invalid">
                        {errors.subscriberReference?.message}
                      </Form.Control.Feedback>
                    </Form.Group>

                    {/* Amount */}
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold">Amount (LKR) *</Form.Label>
                      <Form.Control
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        {...register('amount', {
                          required: 'Payment amount is required',
                          min: { value: 1, message: 'Minimum bill payment is LKR 1.00' },
                          max: { value: 1000000, message: 'Maximum limit is LKR 1,000,000.00' },
                        })}
                        isInvalid={!!errors.amount}
                      />
                      <Form.Control.Feedback type="invalid">
                        {errors.amount?.message}
                      </Form.Control.Feedback>
                    </Form.Group>

                    {/* Remarks */}
                    <Form.Group className="mb-4">
                      <Form.Label className="fw-semibold">Remarks (Optional)</Form.Label>
                      <Form.Control
                        type="text"
                        placeholder="e.g. March 2026 Home Electricity"
                        {...register('remarks', { maxLength: { value: 300, message: 'Max 300 characters' } })}
                      />
                    </Form.Group>

                    <Button variant="primary" type="submit" className="w-100 fw-bold py-2">
                      Review Payment Details <i className="bi bi-arrow-right ms-1"></i>
                    </Button>
                  </Form>
                )}
              </Card.Body>
            </Card>
          )}

          {/* STEP 2: Review & Confirm */}
          {step === 2 && reviewData && (
            <Card className="shadow-sm border-0">
              <Card.Header className="bg-light fw-bold py-3">
                <i className="bi bi-shield-check me-2 text-primary"></i>Confirm Bill Payment
              </Card.Header>
              <Card.Body className="p-4">
                <p className="text-muted small mb-3">
                  Please verify all bill payment details carefully before authorizing debit.
                </p>

                <Table borderless className="mb-4">
                  <tbody>
                    <tr>
                      <td className="text-muted">Debit From:</td>
                      <td className="text-end fw-bold">
                        <code>{reviewData.account.accountNumber}</code> ({reviewData.account.type})
                      </td>
                    </tr>
                    <tr>
                      <td className="text-muted">Biller:</td>
                      <td className="text-end fw-bold text-primary">
                        {reviewData.biller?.name}
                      </td>
                    </tr>
                    <tr>
                      <td className="text-muted">Category:</td>
                      <td className="text-end">{reviewData.biller?.category}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Subscriber Reference:</td>
                      <td className="text-end fw-bold"><code>{reviewData.subscriberReference}</code></td>
                    </tr>
                    <tr>
                      <td className="text-muted">Payment Amount:</td>
                      <td className="text-end fw-bold text-success fs-5">
                        {formatMoney(reviewData.amountNum)}
                      </td>
                    </tr>
                    {reviewData.remarks && (
                      <tr>
                        <td className="text-muted">Remarks:</td>
                        <td className="text-end">{reviewData.remarks}</td>
                      </tr>
                    )}
                  </tbody>
                </Table>

                <div className="d-flex gap-2">
                  <Button
                    variant="outline-secondary"
                    className="w-50"
                    onClick={() => setStep(1)}
                    disabled={submitting}
                  >
                    Back to Edit
                  </Button>
                  <Button
                    variant="success"
                    className="w-50 fw-bold"
                    onClick={handleConfirmPayment}
                    disabled={submitting}
                  >
                    {submitting ? (
                      <>
                        <Spinner size="sm" className="me-2" /> Authorizing...
                      </>
                    ) : (
                      'Authorize Payment'
                    )}
                  </Button>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* STEP 3: Receipt */}
          {step === 3 && receiptData && (
            <Card className="shadow-sm border-0">
              <Card.Header className="bg-success text-white fw-bold py-3 text-center">
                <i className="bi bi-check-circle-fill me-2 fs-5"></i>Payment Successful
              </Card.Header>
              <Card.Body className="p-4">
                <div className="text-center mb-4">
                  <h3 className="fw-bold text-success mb-1">{formatMoney(receiptData.amount)}</h3>
                  <div className="text-muted small">Paid to {receiptData.billerName}</div>
                  <Badge bg="light" text="dark" className="border mt-2">
                    Ref: {receiptData.reference}
                  </Badge>
                </div>

                <Table borderless className="mb-4">
                  <tbody>
                    <tr>
                      <td className="text-muted">Paid From:</td>
                      <td className="text-end fw-bold"><code>{receiptData.sourceAccountNumber}</code></td>
                    </tr>
                    <tr>
                      <td className="text-muted">Biller:</td>
                      <td className="text-end">{receiptData.billerName}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Subscriber Ref:</td>
                      <td className="text-end fw-bold"><code>{receiptData.subscriberReference}</code></td>
                    </tr>
                    <tr>
                      <td className="text-muted">Updated Balance:</td>
                      <td className="text-end fw-bold text-primary">
                        {formatMoney(receiptData.balanceAfter)}
                      </td>
                    </tr>
                    <tr>
                      <td className="text-muted">Timestamp:</td>
                      <td className="text-end">{formatDateTime(receiptData.timestamp)}</td>
                    </tr>
                  </tbody>
                </Table>

                <div className="d-flex flex-column gap-2">
                  <Button variant="primary" onClick={handleStartNewPayment}>
                    <i className="bi bi-plus-circle me-1"></i> Make Another Bill Payment
                  </Button>
                  <Button
                    as={Link}
                    to={PATHS.BILL_PAYMENT_HISTORY}
                    variant="outline-secondary"
                  >
                    View Bill Payment History
                  </Button>
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>
    </div>
  );
}
