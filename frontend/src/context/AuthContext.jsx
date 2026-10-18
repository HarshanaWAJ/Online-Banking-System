import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Spinner } from 'react-bootstrap';
import httpClient from '../api/httpClient.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchCurrentUser = useCallback(async () => {
    try {
      const response = await httpClient.get('/auth/me');
      setUser(response.data.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCurrentUser();
  }, [fetchCurrentUser]);

  const login = useCallback(async (credentials) => {
    const response = await httpClient.post('/auth/login', credentials);
    setUser(response.data.data);
    return response.data.data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await httpClient.post('/auth/logout');
    } catch {
      /* ignore */
    }
    setUser(null);
    window.location.href = '/login';
  }, []);

  const updateUser = useCallback((data) => {
    setUser((prev) => ({ ...prev, ...data }));
  }, []);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" role="status" variant="primary">
          <span className="visually-hidden">Loading...</span>
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

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export default AuthContext;
