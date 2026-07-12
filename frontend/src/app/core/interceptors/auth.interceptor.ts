import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { API_PATHS } from '../constants/api-paths';
import { APP_PATHS } from '../constants/routes';
import { environment } from '../../../environments/environment';

/**
 * Attaches Bearer JWT and performs a single-flight refresh on 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const isAuthEndpoint = isPublicAuthRequest(req.url);

  if (isAuthEndpoint) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      return authService.refresh().pipe(
        switchMap((response) => {
          const retry = req.clone({
            setHeaders: { Authorization: `Bearer ${response.accessToken}` },
          });
          return next(retry);
        }),
        catchError((refreshError) => {
          authService.clearSession();
          void router.navigateByUrl(APP_PATHS.login);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};

function isPublicAuthRequest(url: string): boolean {
  const base = environment.apiBaseUrl;
  return (
    url.includes(`${base}/${API_PATHS.auth.login}`) ||
    url.includes(`${base}/${API_PATHS.auth.google}`) ||
    url.includes(`${base}/${API_PATHS.auth.refresh}`) ||
    url.includes(`${base}/${API_PATHS.auth.logout}`)
  );
}
