import { DatePipe } from '@angular/common';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, firstValueFrom } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { AuthService } from '../../../auth/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../components/confirm-dialog/confirm-dialog';
import {
  ResetPasswordDialog,
  ResetPasswordDialogData,
} from '../../components/reset-password-dialog/reset-password-dialog';
import { ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-detail-page',
  standalone: true,
  imports: [PageHeader, Button, RouterLink, DatePipe],
  templateUrl: './user-detail-page.html',
  styleUrl: './user-detail-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);

  readonly paths = APP_PATHS;
  readonly loading = signal(true);
  readonly user = signal<ManagedUser | null>(null);
  readonly acting = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigateByUrl(APP_PATHS.users);
      return;
    }
    this.load(id);
  }

  isSelf(): boolean {
    return this.user()?.id === this.authService.currentUser()?.id;
  }

  async toggleEnabled(): Promise<void> {
    const current = this.user();
    if (!current || this.isSelf()) {
      return;
    }
    const enabling = !current.enabled;
    const ref = this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialog, {
      data: {
        title: enabling ? 'Enable user' : 'Disable user',
        message: enabling
          ? `Enable account for ${current.username}?`
          : `Disable account for ${current.username}? Active sessions will be revoked.`,
        confirmLabel: enabling ? 'Enable' : 'Disable',
        danger: !enabling,
      },
    });
    if (!(await firstValueFrom(ref.closed))) {
      return;
    }

    this.acting.set(true);
    const request$ = enabling
      ? this.userService.enable(current.id)
      : this.userService.disable(current.id);
    request$.pipe(finalize(() => this.acting.set(false))).subscribe({
      next: (user) => {
        this.user.set(user);
        this.notifications.success(enabling ? 'User enabled.' : 'User disabled.');
      },
      error: (err: HttpErrorResponse) =>
        this.notifications.error(err.error?.message || 'Action failed.'),
    });
  }

  async resetPassword(): Promise<void> {
    const current = this.user();
    if (!current) {
      return;
    }
    const ref = this.dialog.open<string | null, ResetPasswordDialogData>(
      ResetPasswordDialog,
      { data: { username: current.username } },
    );
    const newPassword = await firstValueFrom(ref.closed);
    if (!newPassword) {
      return;
    }

    this.acting.set(true);
    this.userService
      .resetPassword(current.id, { newPassword })
      .pipe(finalize(() => this.acting.set(false)))
      .subscribe({
        next: (user) => {
          this.user.set(user);
          this.notifications.success('Password reset successfully.');
        },
        error: (err: HttpErrorResponse) =>
          this.notifications.error(err.error?.message || 'Password reset failed.'),
      });
  }

  private load(id: string): void {
    this.loading.set(true);
    this.userService
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => this.user.set(user),
        error: (err: HttpErrorResponse) => {
          this.notifications.error(err.error?.message || 'User not found.');
          void this.router.navigateByUrl(APP_PATHS.users);
        },
      });
  }
}
