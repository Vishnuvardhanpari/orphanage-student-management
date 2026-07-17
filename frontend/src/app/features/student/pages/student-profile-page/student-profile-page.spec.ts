import { Dialog } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthService } from '../../../auth/services/auth.service';
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
  let dialog: jasmine.SpyObj<Dialog>;

  async function setup(options?: {
    routeId?: string | null;
    archived?: boolean;
    student?: StudentDetail;
    documents?: StudentDocumentMeta[];
    getByIdError?: boolean;
    photoError?: boolean;
    isAdmin?: boolean;
    /** Leaves getById unresolved to observe the pre-load state. */
    pending?: boolean;
  }): Promise<void> {
    const routeId = options?.routeId === undefined ? studentId : options.routeId;
    studentService = jasmine.createSpyObj('StudentService', [
      'getById',
      'listDocuments',
      'fetchPhoto',
      'downloadDocument',
      'softDelete',
      'restore',
    ]);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    if (options?.pending) {
      studentService.getById.and.returnValue(new Subject());
      studentService.listDocuments.and.returnValue(new Subject());
    } else if (options?.getByIdError) {
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
    studentService.softDelete.and.returnValue(of(undefined));
    studentService.restore.and.returnValue(of(studentDetail));

    const authService = {
      isAdmin: () => options?.isAdmin ?? true,
    };

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
              data: { archived: !!options?.archived },
            },
          },
        },
        { provide: StudentService, useValue: studentService },
        { provide: NotificationService, useValue: notifications },
        { provide: AuthService, useValue: authService },
        { provide: Dialog, useValue: (dialog = jasmine.createSpyObj('Dialog', ['open'])) },
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

  it('hides edit navigation when archived and shows restore for admin', async () => {
    await setup({
      archived: true,
      isAdmin: true,
      student: { ...studentDetail, status: StudentStatus.Inactive },
    });

    expect(page.archived()).toBeTrue();
    page.editStudent();
    expect(router.navigateByUrl).not.toHaveBeenCalledWith(
      `${APP_PATHS.students}/${studentId}/edit`,
    );

    page.goBack();
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.studentsInactive);
  });

  // Regression for QA BUG-001: archived mode must be derived from the loaded
  // student's actual status, not from which route was used to reach the page.
  it('derives archived state from student status even when reached via the active route', async () => {
    await setup({
      archived: false,
      isAdmin: true,
      student: { ...studentDetail, status: StudentStatus.Inactive },
    });

    expect(page.archived()).toBeTrue();
    page.editStudent();
    expect(router.navigateByUrl).not.toHaveBeenCalledWith(
      `${APP_PATHS.students}/${studentId}/edit`,
    );
  });

  it('does not treat an active student as archived even when reached via the archived route', async () => {
    await setup({ archived: true, isAdmin: true, student: studentDetail });

    expect(page.archived()).toBeFalse();
  });

  it('falls back to the route hint for "back" navigation before the student loads', async () => {
    await setup({ archived: true, isAdmin: true, pending: true });

    // Before load() resolves, archived() must reflect the route hint so the
    // loading UI (back link/subtitle) does not briefly show the wrong mode.
    expect(page.loading()).toBeTrue();
    expect(page.student()).toBeNull();
    expect(page.archived()).toBeTrue();
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

  // Regression suite for QA BUG-005/BUG-006: the profile page's archive
  // action now opens a dedicated dialog with optional exit details, and
  // neither the archive nor restore flow had test coverage before this pass.
  describe('archiveStudent', () => {
    it('opens the archive dialog with the loaded student details', async () => {
      await setup();
      dialog.open.and.returnValue({ closed: of(null) } as never);

      await page.archiveStudent();

      expect(dialog.open).toHaveBeenCalledWith(
        jasmine.anything(),
        jasmine.objectContaining({
          data: {
            studentName: 'Ravi',
            admissionNumber: studentDetail.admissionNumber,
            admissionDate: studentDetail.admissionDate,
          },
        }),
      );
    });

    it('archives with the provided exit details and navigates to the active list', async () => {
      await setup();
      const exitDetails = {
        exitDate: '2026-01-10',
        exitReason: 'Family relocated',
        exitRemarks: 'Handed over to guardian',
      };
      dialog.open.and.returnValue({ closed: of(exitDetails) } as never);

      await page.archiveStudent();

      expect(studentService.softDelete).toHaveBeenCalledWith(studentId, exitDetails);
      expect(notifications.success).toHaveBeenCalledWith('Student archived.');
      expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.students);
    });

    it('does not archive when the dialog is cancelled', async () => {
      await setup();
      dialog.open.and.returnValue({ closed: of(null) } as never);

      await page.archiveStudent();

      expect(studentService.softDelete).not.toHaveBeenCalled();
    });

    it('does nothing when the student is already archived', async () => {
      await setup({
        archived: true,
        student: { ...studentDetail, status: StudentStatus.Inactive },
      });

      await page.archiveStudent();

      expect(dialog.open).not.toHaveBeenCalled();
    });
  });

  describe('restoreStudent', () => {
    it('restores and navigates to the restored profile when confirmed', async () => {
      await setup({
        archived: true,
        isAdmin: true,
        student: { ...studentDetail, status: StudentStatus.Inactive },
      });
      dialog.open.and.returnValue({ closed: of(true) } as never);

      await page.restoreStudent();

      expect(studentService.restore).toHaveBeenCalledWith(studentId);
      expect(notifications.success).toHaveBeenCalledWith('Student restored.');
      expect(router.navigateByUrl).toHaveBeenCalledWith(`${APP_PATHS.students}/${studentId}`);
    });

    it('does not restore when the confirmation dialog is cancelled', async () => {
      await setup({
        archived: true,
        isAdmin: true,
        student: { ...studentDetail, status: StudentStatus.Inactive },
      });
      dialog.open.and.returnValue({ closed: of(false) } as never);

      await page.restoreStudent();

      expect(studentService.restore).not.toHaveBeenCalled();
    });

    it('does nothing for a non-admin user', async () => {
      await setup({
        archived: true,
        isAdmin: false,
        student: { ...studentDetail, status: StudentStatus.Inactive },
      });

      await page.restoreStudent();

      expect(dialog.open).not.toHaveBeenCalled();
      expect(studentService.restore).not.toHaveBeenCalled();
    });

    it('does nothing for an active (non-archived) student', async () => {
      await setup({ isAdmin: true, student: studentDetail });

      await page.restoreStudent();

      expect(dialog.open).not.toHaveBeenCalled();
    });
  });
});
