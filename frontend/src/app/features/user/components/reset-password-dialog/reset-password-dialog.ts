import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Button } from '../../../../shared/components/button/button';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

export interface ResetPasswordDialogData {
  username: string;
}

function passwordsMatchValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get('newPassword')?.value;
    const confirm = group.get('confirmPassword')?.value;
    if (!password || !confirm) {
      return null;
    }
    return password === confirm ? null : { passwordsMismatch: true };
  };
}

@Component({
  selector: 'app-reset-password-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, Button],
  template: `
    <div class="reset-dialog" role="dialog" aria-labelledby="reset-password-title">
      <h2 id="reset-password-title" class="reset-dialog__title">Reset password</h2>
      <p class="reset-dialog__subtitle">Set a new password for {{ data.username }}.</p>
      <form class="reset-dialog__form" [formGroup]="form" (ngSubmit)="submit()">
        <label class="reset-dialog__field">
          <span>New password</span>
          <input
            type="password"
            formControlName="newPassword"
            autocomplete="new-password"
            class="reset-dialog__input"
          />
          @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
            <span class="reset-dialog__error">Password must be at least 8 characters.</span>
          }
        </label>
        <label class="reset-dialog__field">
          <span>Confirm password</span>
          <input
            type="password"
            formControlName="confirmPassword"
            autocomplete="new-password"
            class="reset-dialog__input"
          />
          @if (form.controls.confirmPassword.touched && form.controls.confirmPassword.hasError('required')) {
            <span class="reset-dialog__error">Confirm password is required.</span>
          }
          @if (
            form.controls.confirmPassword.touched &&
            form.hasError('passwordsMismatch') &&
            !form.controls.confirmPassword.hasError('required')
          ) {
            <span class="reset-dialog__error">Passwords do not match.</span>
          }
        </label>
        @if (error()) {
          <p class="reset-dialog__error" role="alert">{{ error() }}</p>
        }
        <div class="reset-dialog__actions">
          <app-button type="button" variant="secondary" (pressed)="dialogRef.close(null)">
            Cancel
          </app-button>
          <app-button type="submit" variant="primary" [disabled]="form.invalid || submitting()">
            {{ submitting() ? 'Saving…' : 'Reset password' }}
          </app-button>
        </div>
      </form>
    </div>
  `,
  styles: `
    @reference "../../../../../styles.css";
    .reset-dialog {
      @apply min-w-[22rem] max-w-md rounded-xl border border-surface-border bg-surface-elevated p-6 shadow-lg;
    }
    .reset-dialog__title {
      @apply m-0 text-lg font-semibold text-surface-fg;
    }
    .reset-dialog__subtitle {
      @apply mt-1 mb-4 text-sm text-surface-muted-fg;
    }
    .reset-dialog__form {
      @apply flex flex-col gap-4;
    }
    .reset-dialog__field {
      @apply flex flex-col gap-1.5 text-sm font-medium text-surface-fg;
    }
    .reset-dialog__input {
      @apply rounded-lg border border-surface-border bg-surface px-3 py-2 text-sm font-normal
        text-surface-fg focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/30;
    }
    .reset-dialog__error {
      @apply text-sm font-normal text-error-600;
    }
    .reset-dialog__actions {
      @apply flex justify-end gap-2;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordDialog {
  readonly dialogRef = inject(DialogRef<string | null>);
  readonly data = inject<ResetPasswordDialogData>(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator() },
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.controls.newPassword.value);
  }
}
