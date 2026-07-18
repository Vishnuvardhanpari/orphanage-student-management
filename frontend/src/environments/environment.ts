/**
 * Local / development environment configuration.
 * apiBaseUrl uses the Angular dev-server proxy (proxy.conf.json).
 *
 * Report limits should stay aligned with backend `OMS_REPORTS_MAX_*` defaults.
 * Server validation remains authoritative when the values drift.
 */
export const environment = {
  production: false,
  apiBaseUrl: '/api/v1',
  appName: 'Orphanage Management System',
  googleClientId: '',
  reportsMaxSelected: 50,
  reportsMaxFilterResults: 100,
};
