import httpClient from './httpClient.js';

export const reportsApi = {
  getOverview: () => httpClient.get('/reports/overview'),

  downloadAccountsCsv: async () => {
    const res = await httpClient.get('/reports/export/accounts', { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'accounts_report.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  downloadTransactionsCsv: async (from, to) => {
    const params = {};
    if (from) params.from = from;
    if (to) params.to = to;
    const res = await httpClient.get('/reports/export/transactions', { params, responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'transactions_report.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};
