/**
 * Production environment configuration (single-origin Load Balancer).
 *
 * apiBaseUrl is same-origin (`/api/v1`) so the browser calls the custom domain;
 * the Global HTTPS LB routes `/api/*` to Cloud Run.
 *
 * googleClientId stays empty in git. The CD workflow injects the GIS OAuth
 * client ID before `ng build` (see `.github/workflows/cd-deploy.yml`).
 *
 * Report limits should stay aligned with backend `OMS_REPORTS_MAX_*` defaults.
 * Server validation remains authoritative when the values drift.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
  appName: 'Orphanage Management System',
  googleClientId: '',
  reportsMaxSelected: 50,
  reportsMaxFilterResults: 100,
};
