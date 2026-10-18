import axios from 'axios';
import env from '../config/env.js';

/**
 * Pre-configured Axios HTTP client instance for the Online Banking System.
 *
 * Key Architectural & Security Features:
 * 1. Base URL Routing:
 *    Points to the Spring Boot REST API (`/api` by default, proxied or configured via `VITE_API_BASE_URL`).
 *
 * 2. Cookie Credentials (`withCredentials: true`):
 *    Instructs Axios to include browser cookies in cross-origin / same-origin XMLHttpRequests.
 *    This allows the backend to authenticate requests via secure, `HttpOnly`, `SameSite=Lax` JWT cookies.
 *
 * 3. Double-Submit CSRF Protection:
 *    - `xsrfCookieName: 'XSRF-TOKEN'` -> Axios reads the CSRF token written by Spring Security.
 *    - `xsrfHeaderName: 'X-XSRF-TOKEN'` -> Axios attaches this token as a custom HTTP header.
 *    - Spring Security compares the cookie value against the header value to block CSRF exploits.
 *
 * 4. Error Normalization Interceptor:
 *    Standardizes all server error responses into a consistent `{ code, message, details }` structure
 *    attached to `error.apiError` so UI components can display readable alerts effortlessly.
 *
 * 5. Session Expiration Interception (HTTP 401):
 *    Automatically detects session invalidation or expiry, cleanly redirecting the user to `/login`.
 */
const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Global Response Interceptor.
 *
 * Intercepts every incoming HTTP response before it reaches individual calling functions.
 * - Passes successful (2xx) responses directly through.
 * - Handles authentication failures (401) and normalizes backend error bodies.
 */
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Check if the server rejected the request due to an expired or missing session token
    if (error.response?.status === 401) {
      // Prevent infinite redirect loops if the 401 occurred while already on the login page
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }

    // Normalize error response into a uniform shape
    // Expected backend format: { status: 400, error: { code: '...', message: '...', details: [...] } }
    const apiError = error.response?.data?.error || {
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error occurred. Please check your internet connection.',
      details: [],
    };

    // Attach normalized error directly to the error object for downstream consumption
    error.apiError = apiError;

    return Promise.reject(error);
  }
);

export default httpClient;
