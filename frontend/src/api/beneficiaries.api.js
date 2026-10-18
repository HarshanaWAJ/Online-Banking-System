import httpClient from './httpClient.js';

/**
 * Beneficiaries API client.
 */
export const beneficiaryApi = {
  getMyBeneficiaries: () => httpClient.get('/beneficiaries'),
  addBeneficiary: (data) => httpClient.post('/beneficiaries', data),
  updateBeneficiary: (id, data) => httpClient.patch(`/beneficiaries/${id}`, data),
  deleteBeneficiary: (id) => httpClient.delete(`/beneficiaries/${id}`),
};
