import httpClient from './httpClient.js';

export const adminUsersApi = {
  listUsers: (params) => httpClient.get('/admin/users', { params }),
  createStaffUser: (data) => httpClient.post('/admin/users', data),
  updateUserStatus: (id, active) =>
    httpClient.patch(`/admin/users/${id}/status`, null, { params: { active } }),
  updateStaffPermissions: (id, data) =>
    httpClient.put(`/admin/users/${id}/permissions`, data),
};
