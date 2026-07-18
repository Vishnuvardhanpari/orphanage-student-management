/**
 * API path segments relative to environment.apiBaseUrl (/api/v1).
 */
export const API_PATHS = {
  auth: {
    login: 'auth/login',
    google: 'auth/google',
    logout: 'auth/logout',
    refresh: 'auth/refresh',
    me: 'auth/me',
  },
  students: 'students',
  users: 'users',
  reports: 'reports',
  dashboard: 'dashboard',
  audit: 'audit',
} as const;
