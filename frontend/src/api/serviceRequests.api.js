import httpClient from './httpClient.js';

export const serviceRequestsApi = {
  createRequest: (data) => httpClient.post('/service-requests', data),
  getMyRequests: (params) => httpClient.get('/service-requests/my', { params }),
  getRequestById: (id) => httpClient.get(`/service-requests/${id}`),
  searchRequests: (params) => httpClient.get('/service-requests', { params }),
  assignToMe: (id) => httpClient.patch(`/service-requests/${id}/assign`),
  resolveRequest: (id, data) => httpClient.patch(`/service-requests/${id}/resolve`, data),
};
