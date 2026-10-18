import httpClient from './httpClient.js';

/**
 * ============================================================================
 * Transactions & Account Statements API Service
 * ============================================================================
 * 
 * Purpose:
 * Connects the React client with the Spring Boot banking ledger backend for:
 * 1. Querying chronological ledger records and running balance snapshots.
 * 2. Streaming binary PDF and CSV bank statements directly to client browser storage.
 * ============================================================================
 */
export const transactionsApi = {
  /**
   * Fetches paginated ledger transactions for a given bank account.
   * 
   * @param {number|string} accountId - Database ID of the bank account
   * @param {Object} params - Query parameters:
   *   - page: Zero-indexed page number
   *   - size: Page size limit
   *   - from: Optional ISO-8601 start timestamp
   *   - to: Optional ISO-8601 end timestamp
   * @returns {Promise} Axios response containing standardized ApiResponse
   */
  getTransactions: (accountId, params) =>
    httpClient.get(`/accounts/${accountId}/transactions`, { params }),

  /**
   * Downloads a generated official account statement file (PDF or CSV).
   * 
   * Implementation Details:
   * 1. Uses `responseType: 'blob'` so Axios reads the raw binary stream without string corruption.
   * 2. Wraps binary stream into a browser Blob with MIME type ('application/pdf' or 'text/csv').
   * 3. Creates a temporary DOM anchor tag (`<a>`) with an object URL to trigger native download.
   * 4. Cleans up DOM anchor and calls `URL.revokeObjectURL()` to prevent client memory leaks.
   * 
   * @param {number|string} accountId - Database ID of the bank account
   * @param {string} format - File format: 'pdf' (default) or 'csv'
   * @param {string} [from] - Optional ISO-8601 start date
   * @param {string} [to] - Optional ISO-8601 end date
   */
  downloadStatement: async (accountId, format = 'pdf', from, to) => {
    const params = { format };
    if (from) params.from = from;
    if (to) params.to = to;

    // Request raw binary stream from Spring Boot backend
    const response = await httpClient.get(`/accounts/${accountId}/statement`, {
      params,
      responseType: 'blob',
    });

    // Determine correct MIME type
    const mimeType = format === 'csv' ? 'text/csv;charset=utf-8;' : 'application/pdf';
    const blob = new Blob([response.data], { type: mimeType });

    // Generate local Object URL for download
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `statement_${accountId}.${format}`);
    document.body.appendChild(link);
    link.click();

    // Cleanup temporary DOM element and revoke memory reference
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};
