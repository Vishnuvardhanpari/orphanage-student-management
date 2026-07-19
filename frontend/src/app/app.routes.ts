import { Routes } from '@angular/router';
import { MainLayout } from './layout/pages/main-layout/main-layout';
import { APP_ROUTES } from './core/constants/routes';
import { authGuard } from './core/guards/auth.guard';
import { ErrorPage } from './shared/components/error-page/error-page';

export const routes: Routes = [
  {
    path: APP_ROUTES.auth,
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: APP_ROUTES.dashboard,
      },
      {
        path: APP_ROUTES.dashboard,
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(
            (m) => m.DASHBOARD_ROUTES,
          ),
      },
      {
        path: APP_ROUTES.students,
        loadChildren: () =>
          import('./features/student/student.routes').then(
            (m) => m.STUDENT_ROUTES,
          ),
      },
      {
        path: APP_ROUTES.reports,
        loadChildren: () =>
          import('./features/report/report.routes').then((m) => m.REPORT_ROUTES),
      },
      {
        path: APP_ROUTES.users,
        loadChildren: () =>
          import('./features/user/user.routes').then((m) => m.USER_ROUTES),
      },
      {
        path: APP_ROUTES.audit,
        loadChildren: () =>
          import('./features/audit/audit.routes').then((m) => m.AUDIT_ROUTES),
      },
      {
        path: '**',
        component: ErrorPage,
      },
    ],
  },
  {
    path: '**',
    component: ErrorPage,
  },
];
