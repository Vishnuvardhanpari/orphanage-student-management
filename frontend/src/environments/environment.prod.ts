/**
 * Production environment configuration.
 * Replace apiBaseUrl with the Cloud Run backend URL before Milestone 15 deployment.
 *
 * Report limits should stay aligned with backend `OMS_REPORTS_MAX_*` defaults.
 * Server validation remains authoritative when the values drift.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://YOUR_CLOUD_RUN_URL/api/v1',
  appName: 'Orphanage Management System',
  googleClientId: '',
  reportsMaxSelected: 50,
  reportsMaxFilterResults: 100,
};
