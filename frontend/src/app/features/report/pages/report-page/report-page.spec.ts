import { Dialog } from '@angular/cdk/dialog';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageResponse } from '../../../../shared/models/page.models';
import {
  Gender,
  StudentStatus,
  StudentSummary,
} from '../../../student/models/student.models';
import { StudentService } from '../../../student/services/student.service';
import {
  buildSelectionPreviewDetails,
  summarizeReportFilters,
} from '../../models/report.models';
import { ReportService } from '../../services/report.service';
import { ReportPage } from './report-page';

describe('ReportPage', () => {
  let fixture: ComponentFixture<ReportPage>;
  let page: ReportPage;
  let studentService: jasmine.SpyObj<StudentService>;
  let reportService: jasmine.SpyObj<ReportService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<Dialog>;

  const summary: StudentSummary = {
    id: '11111111-1111-1111-1111-111111111111',
    admissionNumber: 'ADM-100',
    firstName: 'Anita',
    lastName: 'Sharma',
    gender: Gender.Female,
    dateOfBirth: '2014-01-01',
    status: StudentStatus.Active,
    schoolName: null,
    standard: null,
    admissionDate: '2024-06-01',
    deletedDate: null,
  };

  function pageOf(
    content: StudentSummary[],
    totalElements = content.length,
  ): PageResponse<StudentSummary> {
    return {
      content,
      totalElements,
      totalPages: Math.ceil(totalElements / 20) || 0,
      size: 1,
      number: 0,
      first: true,
      last: true,
      empty: content.length === 0,
    };
  }

  beforeEach(async () => {
    studentService = jasmine.createSpyObj('StudentService', ['list', 'listInactive']);
    reportService = jasmine.createSpyObj('ReportService', [
      'exportStudent',
      'exportFiltered',
    ]);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'warning',
      'error',
    ]);
    dialog = jasmine.createSpyObj('Dialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [ReportPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: StudentService, useValue: studentService },
        { provide: ReportService, useValue: reportService },
        { provide: NotificationService, useValue: notifications },
        { provide: Dialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('looks up a student by exact admission number before export', async () => {
    studentService.list.and.returnValue(of(pageOf([summary], 1)));
    dialog.open.and.returnValue({
      closed: of(false),
    } as never);

    page.singleForm.controls.admissionNumber.setValue('adm-100');
    await page.exportByAdmissionNumber();

    expect(studentService.list).toHaveBeenCalledWith({
      admissionNumber: 'adm-100',
      page: 0,
      size: 1,
    });
    expect(dialog.open).toHaveBeenCalled();
    expect(reportService.exportStudent).not.toHaveBeenCalled();
  });

  it('shows an error when exact admission lookup finds nobody', async () => {
    studentService.list.and.returnValue(of(pageOf([], 0)));

    page.singleForm.controls.admissionNumber.setValue('MISSING');
    await page.exportByAdmissionNumber();

    expect(notifications.error).toHaveBeenCalledWith(
      'No active student found with that admission number.',
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('previews match count and blocks zero-match filtered export', async () => {
    studentService.list.and.returnValue(of(pageOf([], 0)));

    await page.exportFiltered();

    expect(studentService.list).toHaveBeenCalled();
    expect(notifications.warning).toHaveBeenCalledWith(
      'No students match the selected filters.',
    );
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('rejects inverted age range before calling list APIs', async () => {
    page.filterForm.patchValue({ ageMin: 15, ageMax: 10 });

    await page.exportFiltered();

    expect(notifications.warning).toHaveBeenCalledWith(
      'Minimum age cannot be greater than maximum age.',
    );
    expect(studentService.list).not.toHaveBeenCalled();
  });

  it('summarizeReportFilters includes scope', () => {
    expect(summarizeReportFilters({ scope: 'ARCHIVED', gender: 'MALE' })).toEqual([
      'Scope: Archived students',
      'Gender: MALE',
    ]);
  });

  it('buildSelectionPreviewDetails appends +N more', () => {
    const selected = Array.from({ length: 12 }, (_, i) => ({
      id: String(i),
      admissionNumber: `ADM-${i}`,
      firstName: `S${i}`,
      lastName: null,
    }));
    const details = buildSelectionPreviewDetails(selected, 10);
    expect(details.length).toBe(11);
    expect(details[10]).toBe('+2 more');
  });
});
