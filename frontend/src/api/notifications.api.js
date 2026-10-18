import httpClient from './httpClient.js';

/**
 * Notifications API service.
 */
export const notificationApi = {
  getNotifications: (params) => httpClient.get('/notifications', { params }),
  getUnreadNotifications: () => httpClient.get('/notifications/unread'),
  getUnreadCount: () => httpClient.get('/notifications/unread-count'),
  markAsRead: (id) => httpClient.patch(`/notifications/${id}/read`),
  markAllAsRead: () => httpClient.post('/notifications/read-all'),
};
