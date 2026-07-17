/** Application route path segments (no leading slashes). */
export const APP_ROUTES = {
  auth: 'auth',
  login: 'login',
  dashboard: 'dashboard',
  students: 'students',
  reports: 'reports',
  users: 'users',
} as const;

/** Absolute path helpers for routerLink / navigate. */
export const APP_PATHS = {
  login: `/${APP_ROUTES.auth}/${APP_ROUTES.login}`,
  dashboard: `/${APP_ROUTES.dashboard}`,
  students: `/${APP_ROUTES.students}`,
  studentsInactive: `/${APP_ROUTES.students}/inactive`,
  reports: `/${APP_ROUTES.reports}`,
  users: `/${APP_ROUTES.users}`,
} as const;
