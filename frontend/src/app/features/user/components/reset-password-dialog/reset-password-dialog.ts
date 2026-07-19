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
import { DialogShell } from '../../../../shared/components/dialog-shell/dialog-shell';
import { Field } from '../../../../shared/components/field/field';
import { Input } from '../../../../shared/components/input/input';
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
  imports: [ReactiveFormsModule, Button, DialogShell, Field, Input],
  template: `
    <app-dialog-shell ariaLabel="Reset password" size="lg">
      <h2 dialogTitle>Reset password</h2>
      <p class="reset-dialog__subtitle">Set a new password for {{ data.username }}.</p>
      <form class="reset-dialog__form" [formGroup]="form">
        <app-field
          label="New password"
          [error]="
            form.controls.newPassword.touched && form.controls.newPassword.invalid
              ? 'Password must be at least 8 characters.'
              : ''
          "
        >
          <app-input
            type="password"
            formControlName="newPassword"
            autocomplete="new-password"
          />
        </app-field>
        <app-field
          label="Confirm password"
          [error]="confirmPasswordError()"
        >
          <app-input
            type="password"
            formControlName="confirmPassword"
            autocomplete="new-password"
          />
        </app-field>
        @if (error()) {
          <p class="reset-dialog__error" role="alert">{{ error() }}</p>
        }
      </form>
      <div dialogActions>
        <app-button type="button" variant="secondary" (pressed)="dialogRef.close(null)">
          Cancel
        </app-button>
        <app-button
          type="button"
          variant="primary"
          [disabled]="form.invalid || submitting()"
          (pressed)="submit()"
        >
          {{ submitting() ? 'Saving…' : 'Reset password' }}
        </app-button>
      </div>
    </app-dialog-shell>
  `,
  styles: `
    @reference "../../../../../styles.css";
    .reset-dialog__subtitle {
      @apply m-0 mb-4;
    }
    .reset-dialog__form {
      @apply flex flex-col gap-4;
    }
    .reset-dialog__error {
      @apply m-0 text-sm font-normal text-error-600;
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
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatchValidator() },
  );

  protected confirmPasswordError(): string {
    const confirm = this.form.controls.confirmPassword;
    if (!confirm.touched) {
      return '';
    }
    if (confirm.hasError('required')) {
      return 'Confirm password is required.';
    }
    if (this.form.hasError('passwordsMismatch')) {
      return 'Passwords do not match.';
    }
    return '';
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.dialogRef.close(this.form.controls.newPassword.value);
  }
}
