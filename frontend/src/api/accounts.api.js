import httpClient from './httpClient.js';

/**
 * Bank Accounts and Account Requests API service.
 */
export const accountApi = {
  getMyAccounts: () => httpClient.get('/accounts'),
  getAccountById: (id) => httpClient.get(`/accounts/${id}`),
  getMyAccountById: (id) => httpClient.get(`/accounts/${id}`),
  getAccountByIdForStaff: (id) => httpClient.get(`/accounts/${id}`),
  requestAccount: (data) => httpClient.post('/accounts/requests', data),
  cancelAccountRequest: (id) => httpClient.delete(`/accounts/requests/${id}`),

  // Staff endpoints
  searchAccountRequests: (params) => httpClient.get('/accounts/requests', { params }),
  approveAccountRequest: (id) => httpClient.post(`/accounts/requests/${id}/approve`),
  rejectAccountRequest: (id, reason) => httpClient.post(`/accounts/requests/${id}/reject`, { reason }),
  freezeAccount: (id) => httpClient.post(`/accounts/${id}/freeze`),
  unfreezeAccount: (id) => httpClient.post(`/accounts/${id}/unfreeze`),
  closeAccount: (id, reason) => httpClient.post(`/accounts/${id}/close`, { reason }),
  searchAllAccounts: (params) => httpClient.get('/accounts', { params }),
};
