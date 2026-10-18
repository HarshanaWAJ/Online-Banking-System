import httpClient from './httpClient.js';

/**
 * Cash Operations and Compensations API client for Staff / Admin.
 */
export const cashOperationsApi = {
  postCashOperation: (data) => httpClient.post('/cash-operations', data),
  getCashOperations: (params) => httpClient.get('/cash-operations', { params }),
  createCompensation: (data) => httpClient.post('/compensations', data),
};
