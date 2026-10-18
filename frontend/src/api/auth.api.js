import httpClient from './httpClient.js';

/**
 * Authentication and current session API service.
 */
export const authApi = {
  login: (credentials) => httpClient.post('/auth/login', credentials),
  register: (data) => httpClient.post('/auth/register', data),
  logout: () => httpClient.post('/auth/logout'),
  getCurrentUser: () => httpClient.get('/auth/me'),
  changePassword: (data) => httpClient.post('/auth/change-password', data),
};
