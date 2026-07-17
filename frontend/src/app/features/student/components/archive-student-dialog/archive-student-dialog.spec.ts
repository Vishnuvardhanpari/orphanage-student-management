import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ArchiveStudentDialog,
  ArchiveStudentDialogData,
} from './archive-student-dialog';

describe('ArchiveStudentDialog', () => {
  let fixture: ComponentFixture<ArchiveStudentDialog>;
  let dialog: ArchiveStudentDialog;
  let dialogRef: jasmine.SpyObj<DialogRef<unknown>>;

  const data: ArchiveStudentDialogData = {
    studentName: 'Ravi Kumar',
    admissionNumber: 'ADM-1',
    admissionDate: '2024-06-01',
  };

  async function setup(): Promise<void> {
    dialogRef = jasmine.createSpyObj('DialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [ArchiveStudentDialog],
      providers: [
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ArchiveStudentDialog);
    dialog = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('renders the student name and admission number in the confirmation message', async () => {
    await setup();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Ravi Kumar');
    expect(host.textContent).toContain('ADM-1');
  });

  it('closes with null when cancelled', async () => {
    await setup();

    dialog.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith(null);
  });

  // Normal flow (no exit details provided): preserves flags-only behavior.
  it('confirms with all-null exit details when the form is left blank', async () => {
    await setup();

    dialog.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({
      exitDate: null,
      exitReason: null,
      exitRemarks: null,
    });
  });

  // Normal flow: all optional fields filled in.
  it('confirms with trimmed exit details when the form is filled in', async () => {
    await setup();
    dialog.form.setValue({
      exitDate: '2026-01-10',
      exitReason: '  Family relocated  ',
      exitRemarks: '  Handed over to guardian  ',
    });

    dialog.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({
      exitDate: '2026-01-10',
      exitReason: 'Family relocated',
      exitRemarks: 'Handed over to guardian',
    });
  });

  // Edge case: blank-only reason/remarks should normalize to null, not ''.
  it('normalizes whitespace-only reason and remarks to null', async () => {
    await setup();
    dialog.form.setValue({
      exitDate: null,
      exitReason: '   ',
      exitRemarks: '   ',
    });

    dialog.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith({
      exitDate: null,
      exitReason: null,
      exitRemarks: null,
    });
  });

  // Invalid input: exit date before the admission date must be rejected
  // client-side (mirrors the backend BUG-005 validation) without closing.
  it('rejects an exit date before the admission date and does not close', async () => {
    await setup();
    dialog.form.patchValue({ exitDate: '2020-01-01' });

    dialog.confirm();

    expect(dialog.error()).toBe('Exit date must be on or after the admission date.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  // Invalid input: a future exit date must be rejected client-side.
  it('rejects a future exit date and does not close', async () => {
    await setup();
    dialog.form.patchValue({ exitDate: '2099-01-01' });

    dialog.confirm();

    expect(dialog.error()).toBe('Exit date cannot be in the future.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('accepts an exit date equal to the admission date', async () => {
    await setup();
    dialog.form.patchValue({ exitDate: data.admissionDate });

    dialog.confirm();

    expect(dialog.error()).toBeNull();
    expect(dialogRef.close).toHaveBeenCalledWith(
      jasmine.objectContaining({ exitDate: data.admissionDate }),
    );
  });

  it('clears a previous error once a corrected value is confirmed', async () => {
    await setup();
    dialog.form.patchValue({ exitDate: '2099-01-01' });
    dialog.confirm();
    expect(dialog.error()).not.toBeNull();

    dialog.form.patchValue({ exitDate: '2026-01-10' });
    dialog.confirm();

    expect(dialog.error()).toBeNull();
    expect(dialogRef.close).toHaveBeenCalledWith(
      jasmine.objectContaining({ exitDate: '2026-01-10' }),
    );
  });
});
