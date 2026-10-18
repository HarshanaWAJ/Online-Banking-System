import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Spinner } from 'react-bootstrap';
import httpClient from '../api/httpClient.js';

/**
 * Authentication Context instance.
 * Serves as the global pub/sub channel for user authentication state,
 * active user profile, and session management functions (login, logout, updateUser).
 */
const AuthContext = createContext(null);

/**
 * AuthProvider component wraps the entire React component tree to provide
 * global authentication state and lifecycle actions.
 *
 * Security Model:
 * - The application uses HTTP-only JWT cookies for session management.
 * - Raw JWT tokens are NEVER stored in localStorage or JavaScript memory,
 *   which protects against Cross-Site Scripting (XSS) token theft.
 * - On application initial load or page refresh, this provider calls `GET /api/auth/me`
 *   to check if a valid, active session cookie exists on the client.
 */
export function AuthProvider({ children }) {
  // `user`: null when unauthenticated, or an object containing user details (id, email, role, customer info)
  const [user, setUser] = useState(null);

  // `loading`: true while initial session verification (/auth/me) is in-flight.
  // Prevents premature redirects to the login screen before the server responds.
  const [loading, setLoading] = useState(true);

  /**
   * Verifies the client's session against the backend.
   * Sends the HTTP-only cookie automatically via `withCredentials: true`.
   * If valid (HTTP 200), initializes `user` state with authenticated profile.
   * If invalid/expired (HTTP 401), clears `user` to null.
   */
  const fetchCurrentUser = useCallback(async () => {
    try {
      const response = await httpClient.get('/auth/me');
      setUser(response.data.data);
    } catch {
      // 401 Unauthorized or network error indicates no active session
      setUser(null);
    } finally {
      // Unblock UI rendering once authentication status is determined
      setLoading(false);
    }
  }, []);

  // Run session check on initial mount
  useEffect(() => {
    fetchCurrentUser();
  }, [fetchCurrentUser]);

  /**
   * Authenticates the user with email and password credentials.
   *
   * @param {Object} credentials - Object with `email` and `password` properties.
   * @returns {Promise<Object>} The authenticated user profile.
   */
  const login = useCallback(async (credentials) => {
    const response = await httpClient.post('/auth/login', credentials);
    setUser(response.data.data);
    return response.data.data;
  }, []);

  /**
   * Logs out the user.
   * 1. Calls the backend logout endpoint to invalidate the session in `user_session` table.
   * 2. Clears the local user state.
   * 3. Performs a hard browser redirect to `/login` to ensure all in-memory caches are flushed.
   */
  const logout = useCallback(async () => {
    try {
      await httpClient.post('/auth/logout');
    } catch {
      /* Silently ignore errors (e.g. session already expired) to ensure clean client logout */
    }
    setUser(null);
    window.location.href = '/login';
  }, []);

  /**
   * Updates partial user data in state (e.g., when updating profile details or customer settings)
   * without requiring a full re-login or roundtrip to `/auth/me`.
   *
   * @param {Object} data - Key-value pairs to merge into the existing user object.
   */
  const updateUser = useCallback((data) => {
    setUser((prev) => ({ ...prev, ...data }));
  }, []);

  // Display a full-screen loading spinner until initial session check finishes.
  // This guarantees protected routes don't prematurely kick the user to /login.
  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" role="status" variant="primary">
          <span className="visually-hidden">Loading application...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Custom React hook for consuming the AuthContext.
 * Ensures that components calling `useAuth` are properly nested inside an `<AuthProvider>`.
 *
 * @returns {{ user: Object|null, loading: boolean, login: Function, logout: Function, updateUser: Function }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

export default AuthContext;
