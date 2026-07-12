import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Button } from '../../../../shared/components/button/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [Button],
  template: `
    <div class="confirm-dialog" role="dialog" [attr.aria-label]="data.title">
      <h2 class="confirm-dialog__title">{{ data.title }}</h2>
      <p class="confirm-dialog__message">{{ data.message }}</p>
      <div class="confirm-dialog__actions">
        <app-button variant="secondary" (pressed)="dialogRef.close(false)">Cancel</app-button>
        <app-button
          [variant]="data.danger ? 'danger' : 'primary'"
          (pressed)="dialogRef.close(true)"
        >
          {{ data.confirmLabel || 'Confirm' }}
        </app-button>
      </div>
    </div>
  `,
  styles: `
    @reference "../../../../../styles.css";
    .confirm-dialog {
      @apply min-w-[20rem] max-w-md rounded-xl border border-surface-border bg-surface-elevated p-6 shadow-lg;
    }
    .confirm-dialog__title {
      @apply m-0 text-lg font-semibold text-surface-fg;
    }
    .confirm-dialog__message {
      @apply mt-2 mb-6 text-sm text-surface-muted-fg;
    }
    .confirm-dialog__actions {
      @apply flex justify-end gap-2;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly dialogRef = inject(DialogRef<boolean>);
  readonly data = inject<ConfirmDialogData>(DIALOG_DATA);
}
