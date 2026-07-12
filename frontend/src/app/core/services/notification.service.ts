import { Injectable, inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

/**
 * Thin wrapper over ngx-toastr for consistent notification UX.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly toastr = inject(ToastrService);

  success(message: string, title = 'Success'): void {
    this.toastr.success(message, title);
  }

  error(message: string, title = 'Error'): void {
    this.toastr.error(message, title);
  }

  warning(message: string, title = 'Warning'): void {
    this.toastr.warning(message, title);
  }

  info(message: string, title = 'Info'): void {
    this.toastr.info(message, title);
  }
}
