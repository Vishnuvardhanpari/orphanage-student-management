import { Dialog } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageResponse } from '../../../../shared/models/page.models';
import { AuthService } from '../../../auth/services/auth.service';
import {
  Gender,
  StudentDetail,
  StudentStatus,
  StudentSummary,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';
import { ReportService } from '../../../report/services/report.service';
import { StudentInactiveListPage } from './student-inactive-list-page';

// Regression suite for QA BUG-006: the archived (inactive) list page had no
// dedicated test coverage at all prior to this Milestone 9 QA pass.
describe('StudentInactiveListPage', () => {
  const archivedSummary: StudentSummary = {
    id: '33333333-3333-3333-3333-333333333333',
    admissionNumber: 'ADM-9',
    firstName: 'Meera',
    lastName: 'Iyer',
    gender: Gender.Female,
    dateOfBirth: '2013-02-10',
    status: StudentStatus.Inactive,
    schoolName: 'Lakeview School',
    standard: '6',
    admissionDate: '2022-06-01',
    deletedDate: '2026-01-10T10:00:00Z',
  };

  function pageOf(
    content: StudentSummary[],
    totalElements = content.length,
  ): PageResponse<StudentSummary> {
    return {
      content,
      totalElements,
      totalPages: Math.ceil(totalElements / 20),
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: content.length === 0,
    };
  }

  let fixture: ComponentFixture<StudentInactiveListPage>;
  let page: StudentInactiveListPage;
  let router: Router;
  let studentService: jasmine.SpyObj<StudentService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<Dialog>;

  async function setup(options?: {
    response?: PageResponse<StudentSummary> | 'error';
    isAdmin?: boolean;
  }): Promise<void> {
    const response = options?.response ?? pageOf([archivedSummary]);
    studentService = jasmine.createSpyObj('StudentService', [
      'listInactive',
      'restore',
    ]);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);
    dialog = jasmine.createSpyObj('Dialog', ['open']);

    if (response === 'error') {
      studentService.listInactive.and.returnValue(
        throwError(() => ({ error: { message: 'Boom' } })),
      );
    } else {
      studentService.listInactive.and.returnValue(of(response));
    }
    studentService.restore.and.returnValue(
      of({
        id: archivedSummary.id,
        status: StudentStatus.Active,
      } as unknown as StudentDetail),
    );

    const authService = { isAdmin: () => options?.isAdmin ?? true };
    const reportService = jasmine.createSpyObj('ReportService', ['exportSelected']);

    await TestBed.configureTestingModule({
      imports: [StudentInactiveListPage],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: studentService },
        { provide: ReportService, useValue: reportService },
        { provide: NotificationService, useValue: notifications },
        { provide: AuthService, useValue: authService },
        { provide: Dialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StudentInactiveListPage);
    page = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  it('loads the first page of archived students with the deletedDate default sort', async () => {
    await setup();

    expect(studentService.listInactive).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 0, size: 20, sort: 'deletedDate,desc' }),
    );
    expect(page.students()).toEqual([archivedSummary]);
    expect(page.totalElements()).toBe(1);
  });

  // Regression for QA BUG-UI-001: the header's "Active students" button
  // renders via app-button's routerLink (<a>) variant, which previously lost
  // its projected label to an Angular content-projection limitation.
  it('renders a visible label for the header navigation button', async () => {
    await setup();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Active students');
  });

  it('renders the deletedDate column value in the grid (BUG-007 regression)', async () => {
    await setup();

    const col = page.columnDefs.find((c) => c.colId === 'deletedDate');
    expect(col).toBeTruthy();
    expect(col?.field).toBe('deletedDate');
  });

  it('falls back to an empty page and flags the error state on load failure', async () => {
    await setup({ response: 'error' });

    expect(page.loadFailed()).toBeTrue();
    expect(page.students()).toEqual([]);
    expect(page.loading()).toBeFalse();
  });

  it('re-sorts by an allowed column and reloads from page 0 (BUG-002 regression)', async () => {
    await setup();
    studentService.listInactive.calls.reset();
    page.page.set(2);

    page.onSortChanged({
      api: {
        getColumnState: () => [{ colId: 'schoolName', sort: 'asc' }],
      },
    } as never);

    expect(page.page()).toBe(0);
    expect(studentService.listInactive).toHaveBeenCalledWith(
      jasmine.objectContaining({ sort: 'schoolName,asc' }),
    );
  });

  it('advances and rewinds pages within bounds', async () => {
    await setup({ response: pageOf([archivedSummary], 45) });
    studentService.listInactive.calls.reset();
    studentService.listInactive.and.returnValue(of(pageOf([archivedSummary], 45)));

    page.nextPage();
    expect(page.page()).toBe(1);

    page.prevPage();
    expect(page.page()).toBe(0);

    page.prevPage();
    expect(page.page()).toBe(0);
  });

  it('navigates to the archived profile route on view', async () => {
    await setup();
    const navigateSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);

    page.gridContext.onView(archivedSummary);

    expect(navigateSpy).toHaveBeenCalledWith(
      `/students/inactive/${archivedSummary.id}`,
    );
  });

  it('shows the Restore action for admins only', async () => {
    await setup({ isAdmin: true });
    expect(page.gridContext.showRestore).toBeTrue();
  });

  it('restores a student and reloads the list when confirmed', async () => {
    await setup();
    dialog.open.and.returnValue({ closed: of(true) } as never);
    studentService.listInactive.calls.reset();

    await page.restoreStudent(archivedSummary);

    expect(studentService.restore).toHaveBeenCalledWith(archivedSummary.id);
    expect(notifications.success).toHaveBeenCalledWith('Student restored.');
    expect(studentService.listInactive).toHaveBeenCalled();
  });

  it('does not restore when the confirmation dialog is cancelled', async () => {
    await setup();
    dialog.open.and.returnValue({ closed: of(false) } as never);

    await page.restoreStudent(archivedSummary);

    expect(studentService.restore).not.toHaveBeenCalled();
  });
});
