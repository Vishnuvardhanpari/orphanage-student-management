import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StudentFileUpload } from './student-file-upload';

describe('StudentFileUpload', () => {
  let fixture: ComponentFixture<StudentFileUpload>;
  let component: StudentFileUpload;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudentFileUpload],
    }).compileComponents();

    fixture = TestBed.createComponent(StudentFileUpload);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function selectPhoto(file: File): void {
    const event = {
      target: { files: [file], value: '' },
    } as unknown as Event;
    component.onPhotoSelected(event);
  }

  it('shows a preview after selecting a valid photo and emits the file', () => {
    const emitted: (File | null)[] = [];
    component.photoChange.subscribe((file) => emitted.push(file));
    const file = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });

    selectPhoto(file);

    expect(component.photoPreviewUrl()).not.toBeNull();
    expect(emitted).toEqual([file]);
  });

  it('rejects invalid photo types without preview', () => {
    const errors: string[] = [];
    component.validationError.subscribe((message) => errors.push(message));

    selectPhoto(new File(['x'], 'photo.gif', { type: 'image/gif' }));

    expect(component.photoPreviewUrl()).toBeNull();
    expect(errors.length).toBe(1);
  });

  it('clears the stale preview when the parent resets the pending photo (BUG-001)', () => {
    const file = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
    fixture.componentRef.setInput('photo', file);
    selectPhoto(file);
    expect(component.photoPreviewUrl()).not.toBeNull();

    // Parent clears the pending photo, e.g. after a successful replace upload.
    fixture.componentRef.setInput('photo', null);
    fixture.detectChanges();

    expect(component.photoPreviewUrl()).toBeNull();
  });

  it('keeps the preview while a pending photo is still selected', () => {
    const file = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
    fixture.componentRef.setInput('photo', file);
    selectPhoto(file);
    fixture.detectChanges();

    expect(component.photoPreviewUrl()).not.toBeNull();
  });

  it('rejects oversized documents and keeps valid ones', () => {
    const errors: string[] = [];
    const emitted: unknown[][] = [];
    component.validationError.subscribe((message) => errors.push(message));
    component.documentsChange.subscribe((docs) => emitted.push(docs));

    const big = new File([new Uint8Array(1)], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(big, 'size', { value: 11 * 1024 * 1024 });
    const ok = new File(['pdf'], 'ok.pdf', { type: 'application/pdf' });
    const event = {
      target: { files: [big, ok], value: '' },
    } as unknown as Event;

    component.onDocumentsSelected(event);

    expect(errors.length).toBe(1);
    expect(emitted[0].length).toBe(1);
  });
});
