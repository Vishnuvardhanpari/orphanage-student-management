import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { UserRole } from '../../../../core/enums/user-role';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { Field } from '../../../../shared/components/field/field';
import { Input } from '../../../../shared/components/input/input';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Select } from '../../../../shared/components/select/select';
import { AuthProvider } from '../../models/user.models';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-form-page',
  standalone: true,
  imports: [PageHeader, Button, ReactiveFormsModule, Field, Input, Select],
  templateUrl: './user-form-page.html',
  styleUrl: './user-form-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly notifications = inject(NotificationService);

  readonly paths = APP_PATHS;
  readonly roles = UserRole;
  readonly providers = AuthProvider;
  readonly submitting = signal(false);
  readonly loading = signal(false);
  readonly isEdit = signal(false);
  readonly userId = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    role: [UserRole.Staff, Validators.required],
    authProvider: [AuthProvider.Local, Validators.required],
    password: [''],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEdit.set(true);
      this.userId.set(id);
      this.form.controls.authProvider.disable();
      this.form.controls.password.disable();
      this.loadUser(id);
    } else {
      this.form.controls.authProvider.valueChanges.subscribe((provider) => {
        this.applyPasswordValidators(provider);
      });
      this.applyPasswordValidators(this.form.controls.authProvider.value);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.form.getRawValue();

    if (this.isEdit()) {
      const id = this.userId();
      if (!id) {
        return;
      }
      this.userService
        .update(id, {
          username: raw.username.trim(),
          email: raw.email.trim(),
          role: raw.role,
        })
        .pipe(finalize(() => this.submitting.set(false)))
        .subscribe({
          next: (user) => {
            this.notifications.success('User updated.');
            void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}`);
          },
          error: (err: HttpErrorResponse) => this.handleError(err),
        });
      return;
    }

    this.userService
      .create({
        username: raw.username.trim(),
        email: raw.email.trim(),
        role: raw.role,
        authProvider: raw.authProvider,
        password:
          raw.authProvider === AuthProvider.Local ? raw.password : null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (user) => {
          this.notifications.success('User created.');
          void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}`);
        },
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  private loadUser(id: string): void {
    this.loading.set(true);
    this.userService
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.form.patchValue({
            username: user.username,
            email: user.email,
            role: user.role as UserRole,
            authProvider: user.authProvider as AuthProvider,
          });
        },
        error: (err: HttpErrorResponse) => {
          this.handleError(err);
          void this.router.navigateByUrl(APP_PATHS.users);
        },
      });
  }

  private applyPasswordValidators(provider: string): void {
    const control = this.form.controls.password;
    if (provider === AuthProvider.Local) {
      control.setValidators([
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(128),
      ]);
    } else {
      control.clearValidators();
      control.setValue('');
    }
    control.updateValueAndValidity();
  }

  private handleError(err: HttpErrorResponse): void {
    this.notifications.error(err.error?.message || 'Request failed.');
  }
}
