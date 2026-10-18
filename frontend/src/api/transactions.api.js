import httpClient from './httpClient.js';

/**
 * Account Transactions & Statements API client.
 */
export const transactionsApi = {
  getTransactions: (accountId, params) =>
    httpClient.get(`/accounts/${accountId}/transactions`, { params }),

  downloadStatement: async (accountId, format = 'pdf', from, to) => {
    const params = { format };
    if (from) params.from = from;
    if (to) params.to = to;

    const response = await httpClient.get(`/accounts/${accountId}/statement`, {
      params,
      responseType: 'blob',
    });

    const blob = new Blob([response.data], {
      type: format === 'csv' ? 'text/csv;charset=utf-8;' : 'application/pdf',
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `statement_${accountId}.${format}`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};
