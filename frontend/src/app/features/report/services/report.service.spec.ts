import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_PATHS } from '../../../core/constants/api-paths';
import { environment } from '../../../../environments/environment';
import { ReportService } from './report.service';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/${API_PATHS.reports}`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('exportStudent downloads a PDF blob with filename', () => {
    const blob = new Blob(['%PDF'], { type: 'application/pdf' });
    service.exportStudent('abc').subscribe((file) => {
      expect(file.blob).toBe(blob);
      expect(file.fileName).toBe('student-report-ADM.pdf');
    });

    const req = httpMock.expectOne(`${base}/student/abc`);
    expect(req.request.method).toBe('GET');
    req.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="student-report-ADM.pdf"' },
    });
  });

  it('exportSelected posts student ids', () => {
    const blob = new Blob(['%PDF'], { type: 'application/pdf' });
    service.exportSelected(['id-1', 'id-2']).subscribe((file) => {
      expect(file.blob.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${base}/students`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ studentIds: ['id-1', 'id-2'] });
    req.flush(blob);
  });

  it('exportFiltered posts filter body including scope', () => {
    const blob = new Blob(['%PDF'], { type: 'application/pdf' });
    service
      .exportFiltered({ scope: 'ARCHIVED', gender: 'FEMALE', search: 'Anita' })
      .subscribe();

    const req = httpMock.expectOne(`${base}/filter`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      scope: 'ARCHIVED',
      gender: 'FEMALE',
      search: 'Anita',
    });
    req.flush(blob);
  });
});
