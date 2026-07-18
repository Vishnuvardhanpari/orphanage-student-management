import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ExportReportDialog,
  ExportReportDialogData,
} from './export-report-dialog';

describe('ExportReportDialog', () => {
  let fixture: ComponentFixture<ExportReportDialog>;
  let dialog: ExportReportDialog;
  let dialogRef: jasmine.SpyObj<DialogRef<boolean>>;

  const data: ExportReportDialogData = {
    title: 'Export filtered students',
    message: 'Generate a PDF for 3 matching students?',
    details: ['Scope: Active students', 'Gender: FEMALE', '+2 more'],
    confirmLabel: 'Generate PDF',
  };

  async function setup(override?: Partial<ExportReportDialogData>): Promise<void> {
    dialogRef = jasmine.createSpyObj('DialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [ExportReportDialog],
      providers: [
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: { ...data, ...override } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportReportDialog);
    dialog = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('renders title, message, and detail lines', async () => {
    await setup();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Export filtered students');
    expect(host.textContent).toContain('Generate a PDF for 3 matching students?');
    expect(host.textContent).toContain('Scope: Active students');
    expect(host.textContent).toContain('+2 more');
  });

  it('closes with false when cancelled', async () => {
    await setup();

    dialog.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it('closes with true when confirmed', async () => {
    await setup();

    dialog.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
