import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Button } from '../../../../shared/components/button/button';
import { DialogShell } from '../../../../shared/components/dialog-shell/dialog-shell';

export interface ExportReportDialogData {
  title: string;
  message: string;
  /** Optional bullet lines shown before confirm (e.g. filter summary). */
  details?: string[];
  confirmLabel?: string;
}

/**
 * Confirms a PDF export and shows a preview of selection or filters.
 */
@Component({
  selector: 'app-export-report-dialog',
  standalone: true,
  imports: [Button, DialogShell],
  templateUrl: './export-report-dialog.html',
  styleUrl: './export-report-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportReportDialog {
  readonly dialogRef = inject(DialogRef<boolean>);
  readonly data = inject<ExportReportDialogData>(DIALOG_DATA);

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
