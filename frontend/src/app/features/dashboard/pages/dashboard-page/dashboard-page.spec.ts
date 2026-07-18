import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import {
  DashboardGenderCount,
  DashboardMonthlyAdmission,
  DashboardStatusCount,
  DashboardSummary,
} from '../../models/dashboard.models';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardPage } from './dashboard-page';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let dashboardService: jasmine.SpyObj<DashboardService>;

  const emptySummary: DashboardSummary = {
    totalStudents: 0,
    activeStudents: 0,
    inactiveStudents: 0,
    newAdmissions: 0,
    maleStudents: 0,
    femaleStudents: 0,
    recentAdmissions: [],
    recentUpdates: [],
  };

  const summary: DashboardSummary = {
    totalStudents: 3,
    activeStudents: 2,
    inactiveStudents: 1,
    newAdmissions: 1,
    maleStudents: 1,
    femaleStudents: 1,
    recentAdmissions: [
      {
        id: '11111111-1111-1111-1111-111111111111',
        firstName: 'Anita',
        lastName: 'Rao',
        admissionNumber: 'ADM-001',
        admissionDate: '2026-07-01',
        updatedDate: '2026-07-01T10:00:00Z',
      },
    ],
    recentUpdates: [
      {
        id: '11111111-1111-1111-1111-111111111111',
        firstName: 'Anita',
        lastName: 'Rao',
        admissionNumber: 'ADM-001',
        admissionDate: '2026-07-01',
        updatedDate: '2026-07-10T12:00:00Z',
      },
    ],
  };

  const admissions: DashboardMonthlyAdmission[] = [
    { yearMonth: '2026-07', count: 1 },
  ];
  const gender: DashboardGenderCount[] = [
    { gender: 'MALE', count: 1 },
    { gender: 'FEMALE', count: 1 },
    { gender: 'OTHER', count: 0 },
  ];
  const status: DashboardStatusCount[] = [
    { status: 'ACTIVE', count: 2 },
    { status: 'INACTIVE', count: 1 },
  ];

  beforeEach(async () => {
    dashboardService = jasmine.createSpyObj('DashboardService', [
      'getSummary',
      'getAdmissions',
      'getGender',
      'getStatus',
    ]);
    dashboardService.getSummary.and.returnValue(of(summary));
    dashboardService.getAdmissions.and.returnValue(of(admissions));
    dashboardService.getGender.and.returnValue(of(gender));
    dashboardService.getStatus.and.returnValue(of(status));

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
  });

  it('loads summary cards and recent admissions on init', () => {
    expect(dashboardService.getSummary).toHaveBeenCalled();
    expect(dashboardService.getAdmissions).toHaveBeenCalled();
    expect(dashboardService.getGender).toHaveBeenCalled();
    expect(dashboardService.getStatus).toHaveBeenCalled();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Total Students');
    expect(text).toContain('Left Students');
    expect(text).toContain('This month (UTC)');
    expect(text).toContain('Anita Rao');
    expect(text).toContain('ADM-001');
    expect(text).toContain('Quick actions');
  });

  it('titles the status chart as Status Distribution', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Status Distribution');
    expect(text).not.toContain('Student distribution');
  });

  it('exposes accessible names and text summaries for charts', () => {
    const charts = fixture.debugElement.queryAll(By.css('[role="img"]'));
    expect(charts.length).toBe(3);
    expect(charts[0].attributes['aria-label']).toBe('Gender distribution chart');
    expect(charts[1].attributes['aria-label']).toBe('Status distribution chart');
    expect(charts[2].attributes['aria-label']).toBe('Monthly admissions chart');

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Male: 1, Female: 1, Other: 0');
    expect(text).toContain('Active: 2, Left: 1');
    expect(text).toContain('2026-07: 1');
  });

  it('builds chart options using theme token colors (or documented fallbacks)', () => {
    const genderOption = fixture.componentInstance['genderChartOption']();
    const admissionsOption = fixture.componentInstance['admissionsChartOption']();

    expect(genderOption['color']).toEqual(['#2563eb', '#ea580c', '#6b7280']);
    expect(admissionsOption['color']).toEqual(['#2563eb']);
  });

  it('refresh reloads all dashboard endpoints', () => {
    dashboardService.getSummary.calls.reset();
    dashboardService.getAdmissions.calls.reset();
    dashboardService.getGender.calls.reset();
    dashboardService.getStatus.calls.reset();

    fixture.componentInstance['refresh']();
    fixture.detectChanges();

    expect(dashboardService.getSummary).toHaveBeenCalledTimes(1);
    expect(dashboardService.getAdmissions).toHaveBeenCalledTimes(1);
    expect(dashboardService.getGender).toHaveBeenCalledTimes(1);
    expect(dashboardService.getStatus).toHaveBeenCalledTimes(1);
  });

  it('shows empty recent copy when there are no students', () => {
    dashboardService.getSummary.and.returnValue(of(emptySummary));
    dashboardService.getAdmissions.and.returnValue(of([]));
    dashboardService.getGender.and.returnValue(
      of([
        { gender: 'MALE', count: 0 },
        { gender: 'FEMALE', count: 0 },
        { gender: 'OTHER', count: 0 },
      ]),
    );
    dashboardService.getStatus.and.returnValue(
      of([
        { status: 'ACTIVE', count: 0 },
        { status: 'INACTIVE', count: 0 },
      ]),
    );

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('No active students yet.');
    expect(text).toContain('No recent updates.');
  });

  it('shows an error empty state with Retry when loading fails', () => {
    dashboardService.getSummary.and.returnValue(throwError(() => new Error('fail')));
    dashboardService.getAdmissions.and.returnValue(of([]));
    dashboardService.getGender.and.returnValue(of([]));
    dashboardService.getStatus.and.returnValue(of([]));

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Unable to load dashboard');
    expect(text).toContain('Retry');

    dashboardService.getSummary.and.returnValue(of(summary));
    dashboardService.getSummary.calls.reset();
    dashboardService.getAdmissions.calls.reset();
    dashboardService.getGender.calls.reset();
    dashboardService.getStatus.calls.reset();

    const retry = fixture.debugElement
      .queryAll(By.css('app-button'))
      .find((el) => (el.nativeElement.textContent as string).includes('Retry'));
    expect(retry).withContext('Retry button in empty state').toBeDefined();
    retry!.triggerEventHandler('pressed', undefined);
    fixture.detectChanges();

    expect(dashboardService.getSummary).toHaveBeenCalled();
    expect(dashboardService.getAdmissions).toHaveBeenCalled();
    expect(dashboardService.getGender).toHaveBeenCalled();
    expect(dashboardService.getStatus).toHaveBeenCalled();
  });
});
