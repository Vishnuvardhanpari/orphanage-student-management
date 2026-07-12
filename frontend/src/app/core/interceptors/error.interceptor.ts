import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { ApiErrorResponse } from '../models/api-error-response';

/**
 * Maps HTTP failures to user-facing toast notifications.
 * Re-throws so callers can still handle errors locally.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = resolveErrorMessage(error);
      notifications.error(message);
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
