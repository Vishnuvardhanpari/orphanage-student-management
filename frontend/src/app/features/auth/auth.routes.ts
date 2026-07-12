import { Routes } from '@angular/router';
import { LoginPage } from './pages/login-page/login-page';
import { APP_ROUTES } from '../../core/constants/routes';
import { guestGuard } from '../../core/guards/guest.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: APP_ROUTES.login,
    component: LoginPage,
    title: 'Login',
    canActivate: [guestGuard],
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: APP_ROUTES.login,
  },
];
