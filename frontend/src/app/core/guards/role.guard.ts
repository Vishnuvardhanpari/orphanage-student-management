import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';
import { UserRole } from '../enums/user-role';
import { APP_PATHS } from '../constants/routes';

/**
 * Restricts routes to users with one of the required roles (route data.roles).
 * Defaults to ADMIN when roles are not specified.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree([APP_PATHS.login]);
  }

  const requiredRoles = (route.data['roles'] as UserRole[] | undefined) ?? [
    UserRole.Admin,
  ];

  if (authService.hasRole(requiredRoles)) {
    return true;
  }

  return router.createUrlTree([APP_PATHS.dashboard]);
};
