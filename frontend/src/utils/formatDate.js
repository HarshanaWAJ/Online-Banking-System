/**
 * Formats an ISO date string or Date object for localized display.
 *
 * @param {string|Date} value - The date to format
 * @param {Intl.DateTimeFormatOptions} [options] - Optional formatting options
 * @returns {string} Formatted date string (e.g. "Oct 18, 2026")
 */
export function formatDate(value, options) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(
    'en-LK',
    options || { year: 'numeric', month: 'short', day: '2-digit' }
  );
}

/**
 * Formats an ISO timestamp for localized date and time display.
 *
 * @param {string|Date} value - The timestamp to format
 * @returns {string} Formatted date & time string
 */
export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('en-LK', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}
