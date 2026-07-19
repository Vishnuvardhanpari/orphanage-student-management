import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Button } from '../button/button';
import { DialogShell } from '../dialog-shell/dialog-shell';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [Button, DialogShell],
  template: `
    <app-dialog-shell [ariaLabel]="data.title">
      <h2 dialogTitle>{{ data.title }}</h2>
      <p class="confirm-dialog__message">{{ data.message }}</p>
      <div dialogActions>
        <app-button variant="secondary" (pressed)="dialogRef.close(false)">Cancel</app-button>
        <app-button
          [variant]="data.danger ? 'danger' : 'primary'"
          (pressed)="dialogRef.close(true)"
        >
          {{ data.confirmLabel || 'Confirm' }}
        </app-button>
      </div>
    </app-dialog-shell>
  `,
  styles: `
    @reference "../../../../styles.css";
    .confirm-dialog__message {
      @apply m-0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly dialogRef = inject(DialogRef<boolean>);
  readonly data = inject<ConfirmDialogData>(DIALOG_DATA);
}
