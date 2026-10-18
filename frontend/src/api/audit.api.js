import httpClient from './httpClient.js';

export const auditApi = {
  getAuditEvents: (params) => httpClient.get('/audit-events', { params }),
};
