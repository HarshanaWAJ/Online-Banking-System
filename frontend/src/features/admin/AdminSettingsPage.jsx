import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Form, InputGroup, Row, Spinner } from 'react-bootstrap';
import { adminSettingsApi } from '../../api/adminSettings.api.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function AdminSettingsPage() {
  const { showSuccess, showError } = useToast();

  const [settings, setSettings] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const fetchSettings = async () => {
    try {
      setLoading(true);
      setError('');
      const res = await adminSettingsApi.getSettings();
      const list = res.data.data || [];
      const map = {};
      list.forEach((item) => {
        map[item.settingKey] = item.settingValue;
      });
      setSettings(map);
    } catch (err) {
      setError(err.apiError?.message || 'Failed to load system settings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  const handleChange = (key, value) => {
    setSettings((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      setError('');
      await adminSettingsApi.updateSettings(settings);
      showSuccess('System settings updated successfully.');
    } catch (err) {
      const msg = err.apiError?.message || 'Failed to update system settings.';
      setError(msg);
      showError(msg);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" role="status" />
        <p className="mt-2 text-muted small">Loading system settings...</p>
      </div>
    );
  }

  return (
    <div className="container-fluid py-2">
      <div className="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
        <div>
          <h2 className="h4 fw-bold mb-1">System Settings & Policies</h2>
          <p className="text-muted small mb-0">
            Configure global financial thresholds, security parameters, and operational controls
          </p>
        </div>
        <div className="d-flex gap-2">
          <Button variant="outline-secondary" size="sm" onClick={fetchSettings} disabled={saving}>
            <i className="bi bi-arrow-clockwise me-1" />
            Reload
          </Button>
          <Button variant="primary" size="sm" onClick={handleSave} disabled={saving}>
            {saving ? (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" className="me-1" />
                Saving...
              </>
            ) : (
              <>
                <i className="bi bi-check2-circle me-1" />
                Save Changes
              </>
            )}
          </Button>
        </div>
      </div>

      {error && (
        <Alert variant="danger" dismissible onClose={() => setError('')} className="mb-4">
          <i className="bi bi-exclamation-triangle-fill me-2" />
          {error}
        </Alert>
      )}

      <Form onSubmit={handleSave}>
        <Row className="g-4">
          {/* Financial Limits */}
          <Col md={6}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-light fw-bold d-flex align-items-center">
                <i className="bi bi-currency-exchange me-2 text-primary" />
                Financial Limits & Rules (LKR)
              </Card.Header>
              <Card.Body>
                <Form.Group className="mb-3" controlId="dailyTransferLimit">
                  <Form.Label className="fw-semibold small">Daily Aggregated Transfer Limit</Form.Label>
                  <InputGroup>
                    <InputGroup.Text>LKR</InputGroup.Text>
                    <Form.Control
                      type="number"
                      step="1000"
                      min="0"
                      value={settings.DAILY_TRANSFER_LIMIT || ''}
                      onChange={(e) => handleChange('DAILY_TRANSFER_LIMIT', e.target.value)}
                    />
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Maximum daily outbound transfer limit per customer account.
                  </Form.Text>
                </Form.Group>

                <Form.Group className="mb-3" controlId="singleTransferLimit">
                  <Form.Label className="fw-semibold small">Single Transaction Limit</Form.Label>
                  <InputGroup>
                    <InputGroup.Text>LKR</InputGroup.Text>
                    <Form.Control
                      type="number"
                      step="1000"
                      min="0"
                      value={settings.SINGLE_TRANSACTION_LIMIT || ''}
                      onChange={(e) => handleChange('SINGLE_TRANSACTION_LIMIT', e.target.value)}
                    />
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Maximum cap for a single outbound transfer transaction.
                  </Form.Text>
                </Form.Group>

                <Form.Group className="mb-3" controlId="minOpeningDeposit">
                  <Form.Label className="fw-semibold small">Minimum Opening Deposit</Form.Label>
                  <InputGroup>
                    <InputGroup.Text>LKR</InputGroup.Text>
                    <Form.Control
                      type="number"
                      step="100"
                      min="0"
                      value={settings.MIN_OPENING_DEPOSIT || ''}
                      onChange={(e) => handleChange('MIN_OPENING_DEPOSIT', e.target.value)}
                    />
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Required initial deposit credit for newly approved accounts.
                  </Form.Text>
                </Form.Group>

                <Form.Group className="mb-0" controlId="savingsInterestRate">
                  <Form.Label className="fw-semibold small">Savings Annual Interest Rate</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type="number"
                      step="0.05"
                      min="0"
                      max="100"
                      value={settings.SAVINGS_INTEREST_RATE || ''}
                      onChange={(e) => handleChange('SAVINGS_INTEREST_RATE', e.target.value)}
                    />
                    <InputGroup.Text>% p.a.</InputGroup.Text>
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Annual interest calculation rate credited monthly to savings accounts.
                  </Form.Text>
                </Form.Group>
              </Card.Body>
            </Card>
          </Col>

          {/* Security & Sessions */}
          <Col md={6}>
            <Card className="shadow-sm h-100">
              <Card.Header className="bg-light fw-bold d-flex align-items-center">
                <i className="bi bi-shield-lock me-2 text-danger" />
                Security & Session Policies
              </Card.Header>
              <Card.Body>
                <Form.Group className="mb-3" controlId="sessionTimeout">
                  <Form.Label className="fw-semibold small">Session Inactivity Timeout</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type="number"
                      min="5"
                      max="1440"
                      value={settings.SESSION_TIMEOUT_MINUTES || ''}
                      onChange={(e) => handleChange('SESSION_TIMEOUT_MINUTES', e.target.value)}
                    />
                    <InputGroup.Text>Minutes</InputGroup.Text>
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Duration of user inactivity before JWT session token expires.
                  </Form.Text>
                </Form.Group>

                <Form.Group className="mb-3" controlId="maxLoginAttempts">
                  <Form.Label className="fw-semibold small">Max Failed Login Attempts</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type="number"
                      min="1"
                      max="20"
                      value={settings.MAX_LOGIN_ATTEMPTS || ''}
                      onChange={(e) => handleChange('MAX_LOGIN_ATTEMPTS', e.target.value)}
                    />
                    <InputGroup.Text>Attempts</InputGroup.Text>
                  </InputGroup>
                  <Form.Text className="text-muted small">
                    Consecutive authentication failures before IP rate-limiting engages.
                  </Form.Text>
                </Form.Group>

                <Form.Group className="mb-0" controlId="operatingCurrency">
                  <Form.Label className="fw-semibold small">System Operating Currency</Form.Label>
                  <Form.Control
                    type="text"
                    disabled
                    value={settings.SYSTEM_CURRENCY || 'LKR'}
                  />
                  <Form.Text className="text-muted small">
                    Sri Lankan Rupee (LKR) standard currency code (system-fixed).
                  </Form.Text>
                </Form.Group>
              </Card.Body>
            </Card>
          </Col>

          {/* Operational Controls & Maintenance */}
          <Col md={12}>
            <Card className="shadow-sm">
              <Card.Header className="bg-light fw-bold d-flex align-items-center">
                <i className="bi bi-gear-wide-connected me-2 text-warning" />
                Operational Status & Service Maintenance
              </Card.Header>
              <Card.Body>
                <div className="d-flex align-items-center justify-content-between p-3 border rounded mb-3 bg-light">
                  <div>
                    <h6 className="fw-bold mb-1">Maintenance Mode</h6>
                    <p className="text-muted small mb-0">
                      When active, customer portal access will display the scheduled maintenance notice.
                    </p>
                  </div>
                  <Form.Check
                    type="switch"
                    id="maintenance-mode-switch"
                    checked={settings.MAINTENANCE_MODE === 'true'}
                    onChange={(e) => handleChange('MAINTENANCE_MODE', e.target.checked ? 'true' : 'false')}
                    label={
                      settings.MAINTENANCE_MODE === 'true' ? (
                        <Badge bg="danger">Active</Badge>
                      ) : (
                        <Badge bg="success">Normal Operations</Badge>
                      )
                    }
                  />
                </div>

                <Form.Group controlId="maintenanceMessage">
                  <Form.Label className="fw-semibold small">Maintenance Banner Notice</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    value={settings.MAINTENANCE_MESSAGE || ''}
                    onChange={(e) => handleChange('MAINTENANCE_MESSAGE', e.target.value)}
                    placeholder="Enter scheduled maintenance notification message to inform users..."
                  />
                  <Form.Text className="text-muted small">
                    This notification will be broadcast to users when maintenance windows are planned.
                  </Form.Text>
                </Form.Group>
              </Card.Body>
              <Card.Footer className="bg-white text-end py-3">
                <Button variant="primary" type="submit" disabled={saving}>
                  {saving ? (
                    <>
                      <Spinner as="span" animation="border" size="sm" role="status" className="me-1" />
                      Saving Settings...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check-lg me-1" />
                      Save System Settings
                    </>
                  )}
                </Button>
              </Card.Footer>
            </Card>
          </Col>
        </Row>
      </Form>
    </div>
  );
}
