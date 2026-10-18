import httpClient from './httpClient.js';

/**
 * Billers and Bill Payments API client.
 */
export const billersApi = {
  // Public/Customer/Staff active billers
  getActiveBillers: () => httpClient.get('/billers'),

  // Admin biller management
  getAllBillers: (params) => httpClient.get('/billers/admin', { params }),
  getBillerById: (id) => httpClient.get(`/billers/${id}`),
  createBiller: (data) => httpClient.post('/billers', data),
  updateBiller: (id, data) => httpClient.put(`/billers/${id}`, data),
  updateBillerStatus: (id, status) =>
    httpClient.patch(`/billers/${id}/status`, null, { params: { status } }),

  // Bill payment operations
  payBill: (data) => httpClient.post('/bill-payments', data),
  getMyBillPayments: (params) => httpClient.get('/bill-payments', { params }),
  getBillPaymentById: (id) => httpClient.get(`/bill-payments/${id}`),
};
