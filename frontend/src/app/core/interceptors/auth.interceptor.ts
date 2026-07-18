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
 *
 * Logout is special-cased: attach Bearer when a token exists (so AUTH/LOGOUT
 * audit can record the actor), but never attempt refresh-on-401 — the session
 * is ending and the endpoint remains permitAll for revoke-without-JWT.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (isAnonymousAuthRequest(req.url)) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  if (isLogoutRequest(req.url)) {
    return next(authReq);
  }

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

/** Login / Google / refresh — never attach Bearer or refresh-on-401. */
function isAnonymousAuthRequest(url: string): boolean {
  const base = environment.apiBaseUrl;
  return (
    url.includes(`${base}/${API_PATHS.auth.login}`) ||
    url.includes(`${base}/${API_PATHS.auth.google}`) ||
    url.includes(`${base}/${API_PATHS.auth.refresh}`)
  );
}

function isLogoutRequest(url: string): boolean {
  return url.includes(`${environment.apiBaseUrl}/${API_PATHS.auth.logout}`);
}
