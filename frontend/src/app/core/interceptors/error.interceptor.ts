import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { ApiErrorResponse } from '../models/api-error-response';

/**
 * Opt-out for requests whose callers show their own contextual error toast
 * (e.g. blob streams whose error bodies cannot be resolved to a message here).
 */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/**
 * Maps HTTP failures to user-facing toast notifications.
 * Re-throws so callers can still handle errors locally.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!req.context.get(SKIP_ERROR_TOAST)) {
        notifications.error(resolveErrorMessage(error));
      }
      return throwError(() => error);
    }),
  );
};

function resolveErrorMessage(error: HttpErrorResponse): string {
  const body = error.error as ApiErrorResponse | string | null;

  if (body && typeof body === 'object' && 'message' in body && body.message) {
    return body.message;
  }

  if (typeof body === 'string' && body.trim().length > 0) {
    return body;
  }

  if (error.status === 0) {
    return 'Unable to reach the server. Check your connection.';
  }

  return error.message || 'An unexpected error occurred.';
}
