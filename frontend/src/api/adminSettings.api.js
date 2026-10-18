import httpClient from './httpClient.js';

/**
 * Admin System Settings API service.
 */
export const adminSettingsApi = {
  getSettings: () => httpClient.get('/admin/settings'),
  updateSettings: (settings) => httpClient.put('/admin/settings', { settings }),
};
