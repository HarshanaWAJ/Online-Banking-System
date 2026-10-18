import httpClient from './httpClient.js';

/**
 * Customers and profile API service.
 */
export const customerApi = {
  getMyProfile: () => httpClient.get('/customers/me'),
  updateMyProfile: (data) => httpClient.patch('/customers/me', data),
  searchCustomers: (params) => httpClient.get('/customers', { params }),
  getCustomerById: (id) => httpClient.get(`/customers/${id}`),
  toggleCustomerStatus: (id, active) => httpClient.patch(`/customers/${id}/status`, null, { params: { active } }),
};
