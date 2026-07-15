import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageResponse } from '../../../../shared/models/page.models';
import {
  Gender,
  StudentStatus,
  StudentSummary,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';
import { StudentListPage, ageFromDateOfBirth } from './student-list-page';

describe('StudentListPage', () => {
  const summary: StudentSummary = {
    id: '11111111-1111-1111-1111-111111111111',
    admissionNumber: 'ADM-1',
    firstName: 'Anita',
    lastName: 'Sharma',
    gender: Gender.Female,
    dateOfBirth: '2014-03-15',
    status: StudentStatus.Active,
    schoolName: 'Green Valley School',
    standard: '5',
    admissionDate: '2024-06-01',
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

  let fixture: ComponentFixture<StudentListPage>;
  let page: StudentListPage;
  let router: Router;
  let studentService: jasmine.SpyObj<StudentService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  async function setup(
    response: PageResponse<StudentSummary> | 'error' = pageOf([summary]),
  ): Promise<void> {
    studentService = jasmine.createSpyObj('StudentService', ['list']);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);
    if (response === 'error') {
      studentService.list.and.returnValue(
        throwError(() => ({ error: { message: 'Boom' } })),
      );
    } else {
      studentService.list.and.returnValue(of(response));
    }

    await TestBed.configureTestingModule({
      imports: [StudentListPage],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: studentService },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StudentListPage);
    page = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  /**
   * Types into a real DOM input so Angular's value accessor runs (for
   * number inputs this is NumberValueAccessor, which emits number | null).
   */
  function typeInto(controlName: string, value: string): void {
    const input = fixture.nativeElement.querySelector(
      `input[formControlName="${controlName}"]`,
    ) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  /** Submits the filter form the way the Apply button does. */
  function submitFilters(): void {
    const form = fixture.nativeElement.querySelector(
      'form.student-list__filters',
    ) as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('loads the first page with default sort on init', async () => {
    await setup();

    expect(studentService.list).toHaveBeenCalledWith(
      jasmine.objectContaining({
        page: 0,
        size: 20,
        sort: 'admissionDate,desc',
      }),
    );
    expect(page.students()).toEqual([summary]);
    expect(page.totalElements()).toBe(1);
    expect(page.filtersActive()).toBeFalse();
  });

  it('applies filters, resets to page 0, and marks filters active', async () => {
    await setup();
    page.page.set(3);
    page.filterForm.patchValue({
      search: '  anita ',
      gender: 'FEMALE',
      admissionYear: 2024,
      ageMin: 8,
      ageMax: 14,
    });

    page.applyFilters();

    expect(page.page()).toBe(0);
    expect(page.filtersActive()).toBeTrue();
    expect(studentService.list).toHaveBeenCalledWith(
      jasmine.objectContaining({
        search: 'anita',
        gender: Gender.Female,
        admissionYear: 2024,
        ageMin: 8,
        ageMax: 14,
        page: 0,
      }),
    );
  });

  it('rejects an invalid age range without calling the API', async () => {
    await setup();
    studentService.list.calls.reset();
    page.filterForm.patchValue({ ageMin: 12, ageMax: 8 });

    page.applyFilters();

    expect(notifications.error).toHaveBeenCalledWith(
      'Minimum age must not exceed maximum age.',
    );
    expect(studentService.list).not.toHaveBeenCalled();
  });

  it('rejects a non-integer admission year typed into the DOM without calling the API', async () => {
    await setup();
    studentService.list.calls.reset();
    typeInto('admissionYear', '2024.5');

    submitFilters();

    expect(notifications.error).toHaveBeenCalledWith(
      'Admission year must be a whole number.',
    );
    expect(studentService.list).not.toHaveBeenCalled();
  });

  it('rejects a non-integer age typed into the DOM without calling the API', async () => {
    await setup();
    studentService.list.calls.reset();
    typeInto('ageMin', '7.5');

    submitFilters();

    expect(notifications.error).toHaveBeenCalledWith(
      'Age filters must be whole numbers.',
    );
    expect(studentService.list).not.toHaveBeenCalled();
  });

  // Regression suite for issue #43: number inputs go through Angular's
  // NumberValueAccessor, which emits number | null (never strings). These
  // specs drive the real DOM inputs instead of patchValue.
  describe('filters typed into real DOM inputs (#43)', () => {
    it('serializes an admission year typed into the number input', async () => {
      await setup();
      studentService.list.calls.reset();
      typeInto('admissionYear', '2024');

      submitFilters();

      expect(studentService.list).toHaveBeenCalledWith(
        jasmine.objectContaining({ admissionYear: 2024, page: 0 }),
      );
    });

    it('serializes an age range typed into the number inputs', async () => {
      await setup();
      studentService.list.calls.reset();
      typeInto('ageMin', '8');
      typeInto('ageMax', '14');

      submitFilters();

      expect(studentService.list).toHaveBeenCalledWith(
        jasmine.objectContaining({ ageMin: 8, ageMax: 14, page: 0 }),
      );
    });

    it('filters by school after a number field was typed into and cleared', async () => {
      await setup();
      studentService.list.calls.reset();
      typeInto('admissionYear', '2024');
      typeInto('admissionYear', '');
      typeInto('school', 'Green Valley');

      submitFilters();

      expect(studentService.list).toHaveBeenCalledWith(
        jasmine.objectContaining({ school: 'Green Valley', page: 0 }),
      );
      const params = studentService.list.calls.mostRecent().args[0];
      expect(params?.admissionYear).toBeUndefined();
      expect(notifications.error).not.toHaveBeenCalled();
    });

    it('filters by school on a fresh form with untouched number fields', async () => {
      await setup();
      studentService.list.calls.reset();
      typeInto('school', 'Green Valley');

      submitFilters();

      expect(studentService.list).toHaveBeenCalledWith(
        jasmine.objectContaining({ school: 'Green Valley', page: 0 }),
      );
    });
  });

  it('clears filters and reloads from page 0', async () => {
    await setup();
    page.filterForm.patchValue({ search: 'x', gender: 'MALE' });
    page.applyFilters();
    studentService.list.calls.reset();

    page.clearFilters();

    expect(page.filterForm.getRawValue().search).toBe('');
    expect(page.filtersActive()).toBeFalse();
    expect(studentService.list).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 0 }),
    );
  });

  it('navigates to view and edit from grid actions', async () => {
    await setup();
    const navigateSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);

    page.gridContext.onView(summary);
    page.gridContext.onEdit(summary);

    expect(navigateSpy).toHaveBeenCalledWith(`/students/${summary.id}`);
    expect(navigateSpy).toHaveBeenCalledWith(`/students/${summary.id}/edit`);
  });

  it('falls back to an empty page and flags the error state on load failure', async () => {
    await setup('error');

    // The global errorInterceptor owns the toast; the page must not add one.
    expect(notifications.error).not.toHaveBeenCalled();
    expect(page.loadFailed()).toBeTrue();
    expect(page.students()).toEqual([]);
    expect(page.totalElements()).toBe(0);
    expect(page.loading()).toBeFalse();
  });

  it('renders the error state instead of "No students yet" on load failure', async () => {
    await setup('error');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Unable to load students');
    expect(text).not.toContain('No students yet');
  });

  it('clears the error state when a retry succeeds', async () => {
    await setup('error');
    expect(page.loadFailed()).toBeTrue();

    studentService.list.and.returnValue(of(pageOf([summary])));
    page.loadStudents();

    expect(page.loadFailed()).toBeFalse();
    expect(page.students()).toEqual([summary]);
  });

  it('advances and rewinds pages within bounds', async () => {
    await setup(pageOf([summary], 45));
    studentService.list.calls.reset();
    studentService.list.and.returnValue(of(pageOf([summary], 45)));

    page.nextPage();
    expect(page.page()).toBe(1);
    expect(studentService.list).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 1 }),
    );

    page.prevPage();
    expect(page.page()).toBe(0);

    page.prevPage();
    expect(page.page()).toBe(0);
  });
});

describe('ageFromDateOfBirth', () => {
  it('computes whole years with birthday boundaries', () => {
    const today = new Date(2026, 6, 15); // 15 Jul 2026
    expect(ageFromDateOfBirth('2016-07-15', today)).toBe(10);
    expect(ageFromDateOfBirth('2016-07-16', today)).toBe(9);
    expect(ageFromDateOfBirth('2016-07-14', today)).toBe(10);
  });
});
