import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  DocumentType,
  Gender,
  StudentDetail,
  StudentDocumentMeta,
  StudentStatus,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';
import { StudentFormPage } from './student-form-page';

describe('StudentFormPage', () => {
  const studentId = '11111111-1111-1111-1111-111111111111';

  const studentDetail: StudentDetail = {
    id: studentId,
    admissionNumber: 'ADM-1',
    firstName: 'Ravi',
    lastName: 'Kumar',
    gender: Gender.Male,
    dateOfBirth: '2015-01-01',
    bloodGroup: null,
    religion: null,
    nationality: null,
    aadhaarNumber: null,
    phoneNumber: null,
    guardianName: 'Guardian',
    guardianRelationship: null,
    guardianPhone: null,
    guardianAddress: null,
    schoolName: null,
    standard: null,
    medium: null,
    previousSchool: null,
    medicalConditions: null,
    allergies: null,
    disability: null,
    emergencyNotes: null,
    admissionDate: '2024-06-01',
    exitDate: null,
    exitReason: null,
    exitRemarks: null,
    status: StudentStatus.Active,
    hasProfilePhoto: false,
    createdDate: '2026-01-01T00:00:00Z',
    updatedDate: '2026-01-01T00:00:00Z',
  };

  const documentMeta: StudentDocumentMeta = {
    id: '22222222-2222-2222-2222-222222222222',
    documentType: DocumentType.AadhaarCard,
    originalFileName: 'aadhaar.pdf',
    contentType: 'application/pdf',
    fileSize: 1024,
    uploadedDate: '2026-01-01T00:00:00Z',
  };

  let fixture: ComponentFixture<StudentFormPage>;
  let page: StudentFormPage;
  let router: Router;
  let studentService: jasmine.SpyObj<StudentService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<Dialog>;

  function stubConfirm(result: boolean | undefined): void {
    dialog.open.and.returnValue(
      { closed: of(result) } as unknown as DialogRef<unknown, unknown>,
    );
  }

  async function setup(options?: {
    routeId?: string | null;
    getByIdError?: boolean;
  }): Promise<void> {
    studentService = jasmine.createSpyObj('StudentService', [
      'create',
      'update',
      'getById',
      'listDocuments',
      'fetchPhoto',
      'replacePhoto',
      'addDocuments',
      'replaceDocument',
      'deletePhoto',
      'deleteDocument',
    ]);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
      'warning',
    ]);
    dialog = jasmine.createSpyObj('Dialog', ['open']);
    stubConfirm(true);

    if (options?.getByIdError) {
      studentService.getById.and.returnValue(throwError(() => ({ status: 404 })));
      studentService.listDocuments.and.returnValue(of([]));
    } else {
      studentService.getById.and.returnValue(of(studentDetail));
      studentService.listDocuments.and.returnValue(of([documentMeta]));
    }
    studentService.fetchPhoto.and.returnValue(of(new Blob(['img'])));
    studentService.update.and.returnValue(of(studentDetail));
    studentService.create.and.returnValue(
      of({
        progress: 100,
        response: {
          id: studentId,
          admissionNumber: 'ADM-1',
          status: StudentStatus.Active,
          createdDate: '2026-01-01T00:00:00Z',
        },
      }),
    );

    const routeId = options?.routeId;
    await TestBed.configureTestingModule({
      imports: [StudentFormPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(routeId ? { id: routeId } : {}),
            },
          },
        },
        { provide: StudentService, useValue: studentService },
        { provide: NotificationService, useValue: notifications },
        { provide: Dialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StudentFormPage);
    page = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  it('stays in create mode without route id', async () => {
    await setup();

    expect(page.mode()).toBe('create');
    expect(page.pageTitle()).toBe('Register student');
    expect(studentService.getById).not.toHaveBeenCalled();
  });

  it('loads student in edit mode and disables admission number', async () => {
    await setup({ routeId: studentId });

    expect(page.mode()).toBe('edit');
    expect(page.pageTitle()).toBe('Edit student');
    expect(studentService.getById).toHaveBeenCalledWith(studentId);
    expect(page.form.controls.admissionNumber.disabled).toBeTrue();
    expect(page.form.controls.firstName.value).toBe('Ravi');
    expect(page.existingDocuments().length).toBe(1);
  });

  it('submits update payload without admission number', async () => {
    await setup({ routeId: studentId });

    page.form.patchValue({ firstName: 'Anita' });
    await page.submit();

    expect(studentService.update).toHaveBeenCalled();
    const [, payload] = studentService.update.calls.mostRecent().args;
    expect(payload.firstName).toBe('Anita');
    expect((payload as { admissionNumber?: string }).admissionNumber).toBeUndefined();
    expect(notifications.success).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith(`${APP_PATHS.students}/${studentId}`);
  });

  it('cancels edit back to profile', async () => {
    await setup({ routeId: studentId });

    page.cancel();

    expect(router.navigateByUrl).toHaveBeenCalledWith(`${APP_PATHS.students}/${studentId}`);
  });

  it('navigates away when edit load fails without a duplicate local toast', async () => {
    await setup({ routeId: studentId, getByIdError: true });

    // The global error interceptor owns the toast (BUG-003).
    expect(notifications.error).not.toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.students);
  });

  it('asks for confirmation before saving with pending media and aborts on cancel (BUG-002)', async () => {
    await setup({ routeId: studentId });
    page.photo.set(new File(['img'], 'new.jpg', { type: 'image/jpeg' }));
    stubConfirm(false);

    await page.submit();

    expect(dialog.open).toHaveBeenCalled();
    expect(studentService.update).not.toHaveBeenCalled();
  });

  it('saves after confirming to discard pending media (BUG-002)', async () => {
    await setup({ routeId: studentId });
    page.photo.set(new File(['img'], 'new.jpg', { type: 'image/jpeg' }));
    stubConfirm(true);

    await page.submit();

    expect(dialog.open).toHaveBeenCalled();
    expect(studentService.update).toHaveBeenCalled();
  });

  it('does not ask for confirmation on save without pending media', async () => {
    await setup({ routeId: studentId });

    await page.submit();

    expect(dialog.open).not.toHaveBeenCalled();
    expect(studentService.update).toHaveBeenCalled();
  });

  it('does not toast locally when update fails (BUG-003)', async () => {
    await setup({ routeId: studentId });
    studentService.update.and.returnValue(throwError(() => ({ status: 409 })));

    await page.submit();

    expect(notifications.error).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalledWith(`${APP_PATHS.students}/${studentId}`);
  });

  it('clears the pending photo after a successful replace (BUG-001)', async () => {
    await setup({ routeId: studentId });
    studentService.replacePhoto.and.returnValue(of(undefined));
    page.photo.set(new File(['img'], 'new.jpg', { type: 'image/jpeg' }));

    page.replacePhoto();

    expect(studentService.replacePhoto).toHaveBeenCalled();
    expect(page.photo()).toBeNull();
    expect(notifications.success).toHaveBeenCalledWith('Profile photo updated.');
  });

  it('tracks photo and document uploads with separate busy flags (BUG-005)', async () => {
    await setup({ routeId: studentId });
    const pendingPhoto$ = new Subject<void>();
    studentService.replacePhoto.and.returnValue(pendingPhoto$.asObservable());
    page.photo.set(new File(['img'], 'new.jpg', { type: 'image/jpeg' }));
    page.documents.set([
      {
        file: new File(['pdf'], 'birth.pdf', { type: 'application/pdf' }),
        documentType: DocumentType.BirthCertificate,
      },
    ]);

    page.replacePhoto();

    expect(page.photoUploading()).toBeTrue();
    expect(page.docsUploading()).toBeFalse();
    expect(page.mediaBusy()).toBeTrue();

    pendingPhoto$.complete();
    expect(page.photoUploading()).toBeFalse();
  });

  it('rejects invalid replacement files before calling the API (BUG-004)', async () => {
    await setup({ routeId: studentId });
    const badFile = new File(['exe'], 'virus.exe', { type: 'application/octet-stream' });
    const event = {
      target: { files: [badFile], value: '' },
    } as unknown as Event;

    page.onReplaceDocumentSelected(documentMeta, event);

    expect(notifications.warning).toHaveBeenCalled();
    expect(studentService.replaceDocument).not.toHaveBeenCalled();
  });

  it('passes the selected document type when replacing (BUG-006)', async () => {
    await setup({ routeId: studentId });
    studentService.replaceDocument.and.returnValue(of(documentMeta));
    const selectEvent = {
      target: { value: DocumentType.IdentityProof },
    } as unknown as Event;
    page.onReplacementTypeChange(documentMeta, selectEvent);

    const file = new File(['pdf'], 'identity.pdf', { type: 'application/pdf' });
    const fileEvent = {
      target: { files: [file], value: '' },
    } as unknown as Event;
    page.onReplaceDocumentSelected(documentMeta, fileEvent);

    expect(studentService.replaceDocument).toHaveBeenCalledWith(
      studentId,
      documentMeta.id,
      file,
      DocumentType.IdentityProof,
    );
  });

  it('removes the existing photo after confirmation (BUG-007)', async () => {
    await setup({ routeId: studentId });
    studentService.deletePhoto.and.returnValue(of(undefined));
    page.existingPhotoUrl.set('blob:photo');
    stubConfirm(true);

    await page.removeExistingPhoto();

    expect(studentService.deletePhoto).toHaveBeenCalledWith(studentId);
    expect(page.existingPhotoUrl()).toBeNull();
    expect(notifications.success).toHaveBeenCalledWith('Profile photo removed.');
  });

  it('does not remove the photo when confirmation is cancelled (BUG-007)', async () => {
    await setup({ routeId: studentId });
    page.existingPhotoUrl.set('blob:photo');
    stubConfirm(false);

    await page.removeExistingPhoto();

    expect(studentService.deletePhoto).not.toHaveBeenCalled();
  });

  it('deletes a document after confirmation and refreshes the list (BUG-007)', async () => {
    await setup({ routeId: studentId });
    studentService.deleteDocument.and.returnValue(of(undefined));
    studentService.listDocuments.and.returnValue(of([]));
    stubConfirm(true);

    await page.deleteExistingDocument(documentMeta);

    expect(studentService.deleteDocument).toHaveBeenCalledWith(studentId, documentMeta.id);
    expect(page.existingDocuments()).toEqual([]);
    expect(notifications.success).toHaveBeenCalledWith('Document deleted.');
  });
});
