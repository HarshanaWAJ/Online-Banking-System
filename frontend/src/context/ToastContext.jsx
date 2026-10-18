import { createContext, useContext, useState, useCallback } from 'react';
import { Toast, ToastContainer } from 'react-bootstrap';

/**
 * ToastContext — app-wide toast notification system.
 * Usage: const { showSuccess, showError, showToast } = useToast();
 */
const ToastContext = createContext(null);

let toastIdCounter = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((message, variant = 'success', title) => {
    const id = ++toastIdCounter;
    const newToast = { id, message, variant, title };
    setToasts((prev) => [...prev, newToast]);
    setTimeout(() => removeToast(id), 5000);
  }, [removeToast]);

  const showSuccess = useCallback(
    (message) => showToast(message, 'success', 'Success'),
    [showToast]
  );

  const showError = useCallback(
    (message) => showToast(message, 'danger', 'Error'),
    [showToast]
  );

  return (
    <ToastContext.Provider value={{ showToast, showSuccess, showError }}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 9999 }}>
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            bg={toast.variant}
            onClose={() => removeToast(toast.id)}
            show
            autohide
            delay={5000}
          >
            {toast.title && (
              <Toast.Header>
                <strong className="me-auto">{toast.title}</strong>
              </Toast.Header>
            )}
            <Toast.Body className={toast.variant === 'danger' || toast.variant === 'success' ? 'text-white' : ''}>
              {toast.message}
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

export default ToastContext;
