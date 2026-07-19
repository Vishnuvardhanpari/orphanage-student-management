import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Button } from '../../../../shared/components/button/button';
import { DialogShell } from '../../../../shared/components/dialog-shell/dialog-shell';
import { Field } from '../../../../shared/components/field/field';
import { Input } from '../../../../shared/components/input/input';
import { Textarea } from '../../../../shared/components/textarea/textarea';
import { SoftDeleteExitDetails } from '../../models/student.models';

export interface ArchiveStudentDialogData {
  studentName: string;
  admissionNumber: string;
  /** ISO date; used to bound the optional exit date on the client (Milestone 9 QA — BUG-005). */
  admissionDate: string;
}

/** Optional exit details captured when archiving a student (Milestone 9 QA — BUG-005). */
export type ArchiveStudentResult = SoftDeleteExitDetails;

/**
 * Confirms archiving a student and optionally captures exit date/reason/
 * remarks in the same step, so staff no longer have to record them separately
 * after the fact (Milestone 9 QA — BUG-005). All fields are optional; leaving
 * them blank preserves the original flags-only archive behavior.
 */
@Component({
  selector: 'app-archive-student-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, Button, DialogShell, Field, Input, Textarea],
  templateUrl: './archive-student-dialog.html',
  styleUrl: './archive-student-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArchiveStudentDialog {
  readonly dialogRef = inject(DialogRef<ArchiveStudentResult | null>);
  readonly data = inject<ArchiveStudentDialogData>(DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly today = toIsoDate(new Date());
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    exitDate: this.fb.control<string | null>(null),
    exitReason: this.fb.nonNullable.control(''),
    exitRemarks: this.fb.nonNullable.control(''),
  });

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const raw = this.form.getRawValue();
    const exitDate = raw.exitDate?.trim() || null;

    if (exitDate) {
      if (exitDate > this.today) {
        this.error.set('Exit date cannot be in the future.');
        return;
      }
      if (exitDate < this.data.admissionDate) {
        this.error.set('Exit date must be on or after the admission date.');
        return;
      }
    }

    this.error.set(null);
    this.dialogRef.close({
      exitDate,
      exitReason: raw.exitReason?.trim() || null,
      exitRemarks: raw.exitRemarks?.trim() || null,
    });
  }
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}
