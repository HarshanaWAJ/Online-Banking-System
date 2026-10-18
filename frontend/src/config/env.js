/**
 * Application environment configuration.
 * This is the single source of truth for all environment variables.
 * All other modules import from here — never read import.meta.env directly.
 */
const env = {
  /** Base URL for all API requests. Proxied to backend in development. */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  /** Whether running in development mode */
  isDev: import.meta.env.DEV === true,
};

export default env;
