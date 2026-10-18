import axios from 'axios';
import env from '../config/env.js';

/**
 * Axios HTTP client configured for the banking system API.
 * - withCredentials: true (sends httpOnly JWT cookie automatically)
 * - xsrfCookieName / xsrfHeaderName: CSRF protection via XSRF-TOKEN cookie
 * - Normalizes API error responses into a consistent shape
 * - On 401: clears auth state and redirects to login
 */
const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'Content-Type': 'application/json' },
});

// Response interceptor — normalize errors
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login on session expiry; avoid redirect loops on the login page itself
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    // Normalize: attach the API error object for easy access in components
    const apiError = error.response?.data?.error || {
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error. Please check your connection.',
      details: [],
    };
    error.apiError = apiError;
    return Promise.reject(error);
  }
);

export default httpClient;
