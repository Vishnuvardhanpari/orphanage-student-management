import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import { environment } from '../../../../environments/environment';
import { DashboardSummary } from '../models/dashboard.models';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/${API_PATHS.dashboard}`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getSummary requests dashboard summary and skips error toast', () => {
    const body: DashboardSummary = {
      totalStudents: 1,
      activeStudents: 1,
      inactiveStudents: 0,
      newAdmissions: 1,
      maleStudents: 0,
      femaleStudents: 1,
      recentAdmissions: [],
      recentUpdates: [],
    };

    service.getSummary().subscribe((summary) => {
      expect(summary.totalStudents).toBe(1);
    });

    const req = httpMock.expectOne(`${base}/summary`);
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    req.flush(body);
  });

  it('getAdmissions requests monthly trend and skips error toast', () => {
    service.getAdmissions().subscribe((rows) => {
      expect(rows.length).toBe(1);
      expect(rows[0].yearMonth).toBe('2026-07');
    });

    const req = httpMock.expectOne(`${base}/admissions`);
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    req.flush([{ yearMonth: '2026-07', count: 3 }]);
  });

  it('getGender requests gender distribution and skips error toast', () => {
    service.getGender().subscribe((rows) => {
      expect(rows[0].gender).toBe('MALE');
    });

    const req = httpMock.expectOne(`${base}/gender`);
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    req.flush([{ gender: 'MALE', count: 2 }]);
  });

  it('getStatus requests status distribution and skips error toast', () => {
    service.getStatus().subscribe((rows) => {
      expect(rows[0].status).toBe('ACTIVE');
    });

    const req = httpMock.expectOne(`${base}/status`);
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    req.flush([{ status: 'ACTIVE', count: 4 }]);
  });
});
