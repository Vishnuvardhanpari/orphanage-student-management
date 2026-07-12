/**
 * Production environment configuration.
 * Replace apiBaseUrl with the Cloud Run backend URL before Milestone 15 deployment.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://YOUR_CLOUD_RUN_URL/api/v1',
  appName: 'Orphanage Management System',
  googleClientId: '',
};
