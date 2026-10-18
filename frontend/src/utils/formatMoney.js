/**
 * Formats a numeric amount as Sri Lankan Rupee currency string.
 * Always renders exactly 2 decimal places.
 *
 * @param {number|string} amount - The amount to format
 * @returns {string} e.g. "LKR 1,250.00"
 */
export function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return 'LKR 0.00';
  }
  const numericVal = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (Number.isNaN(numericVal)) {
    return 'LKR 0.00';
  }
  return (
    'LKR ' +
    numericVal.toLocaleString('en-LK', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
  );
}
