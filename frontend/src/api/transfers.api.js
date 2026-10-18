import httpClient from './httpClient.js';

/**
 * Transfers API client.
 */
export const transferApi = {
  previewTransfer: (data) => httpClient.post('/transfers/preview', data),
  postTransfer: (data) => httpClient.post('/transfers', data),
  getTransferById: (id) => httpClient.get(`/transfers/${id}`),
};
