import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
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
import { StudentProfilePage } from './student-profile-page';

describe('StudentProfilePage', () => {
  const studentId = '11111111-1111-1111-1111-111111111111';
  const documentId = '22222222-2222-2222-2222-222222222222';

  const studentDetail: StudentDetail = {
    id: studentId,
    admissionNumber: 'ADM-1',
    firstName: 'Ravi',
    lastName: null,
    gender: Gender.Male,
    dateOfBirth: '2015-01-01',
    bloodGroup: null,
    religion: null,
    nationality: null,
    aadhaarNumber: null,
    phoneNumber: null,
    guardianName: null,
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
    id: documentId,
    documentType: DocumentType.AadhaarCard,
    originalFileName: 'aadhaar.pdf',
    contentType: 'application/pdf',
    fileSize: 1024,
    uploadedDate: '2026-01-01T00:00:00Z',
  };

  let fixture: ComponentFixture<StudentProfilePage>;
  let page: StudentProfilePage;
  let router: Router;
  let studentService: jasmine.SpyObj<StudentService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  async function setup(options?: {
    routeId?: string | null;
    student?: StudentDetail;
    documents?: StudentDocumentMeta[];
    getByIdError?: boolean;
    photoError?: boolean;
  }): Promise<void> {
    const routeId = options?.routeId === undefined ? studentId : options.routeId;
    studentService = jasmine.createSpyObj('StudentService', [
      'getById',
      'listDocuments',
      'fetchPhoto',
      'downloadDocument',
    ]);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    if (options?.getByIdError) {
      studentService.getById.and.returnValue(throwError(() => ({ status: 404 })));
      studentService.listDocuments.and.returnValue(of([]));
    } else {
      studentService.getById.and.returnValue(of(options?.student ?? studentDetail));
      studentService.listDocuments.and.returnValue(
        of(options?.documents ?? [documentMeta]),
      );
    }
    if (options?.photoError) {
      studentService.fetchPhoto.and.returnValue(
        throwError(() => ({ status: 404 })),
      );
    } else {
      studentService.fetchPhoto.and.returnValue(of(new Blob(['img'])));
    }
    studentService.downloadDocument.and.returnValue(
      of({ blob: new Blob(['pdf']), fileName: 'aadhaar.pdf' }),
    );

    await TestBed.configureTestingModule({
      imports: [StudentProfilePage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(
                routeId ? { id: routeId } : {},
              ),
            },
          },
        },
        { provide: StudentService, useValue: studentService },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StudentProfilePage);
    page = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  it('loads student profile and documents on success', async () => {
    await setup();

    expect(studentService.getById).toHaveBeenCalledWith(studentId);
    expect(studentService.listDocuments).toHaveBeenCalledWith(studentId);
    expect(page.student()?.admissionNumber).toBe('ADM-1');
    expect(page.documents().length).toBe(1);
    expect(page.loading()).toBeFalse();
    expect(page.genderLabel(Gender.Male)).toBe('Male');
  });

  it('navigates away and notifies when student is not found', async () => {
    await setup({ getByIdError: true });

    expect(notifications.error).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.students);
  });

  it('shows empty documents without downloading', async () => {
    await setup({ documents: [] });

    expect(page.documents().length).toBe(0);
    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('No documents');
  });

  it('invokes download for a document', async () => {
    await setup();

    page.download(documentMeta);

    expect(studentService.downloadDocument).toHaveBeenCalledWith(
      studentId,
      documentId,
    );
  });

  it('navigates back to the students list', async () => {
    await setup();

    page.goBack();

    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.students);
  });

  it('navigates to edit page', async () => {
    await setup();

    page.editStudent();

    expect(router.navigateByUrl).toHaveBeenCalledWith(
      `${APP_PATHS.students}/${studentId}/edit`,
    );
  });

  it('notifies when profile photo fetch fails', async () => {
    await setup({
      student: { ...studentDetail, hasProfilePhoto: true },
      photoError: true,
    });

    expect(studentService.fetchPhoto).toHaveBeenCalledWith(studentId);
    expect(notifications.error).toHaveBeenCalledWith(
      'Could not load the student photograph.',
    );
    expect(page.photoUnavailable()).toBeTrue();
    expect(page.photoUrl()).toBeNull();
  });
});
