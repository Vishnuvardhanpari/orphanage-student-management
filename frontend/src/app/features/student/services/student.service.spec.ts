import { TestBed } from '@angular/core/testing';
import { HttpEventType, provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import {
  DocumentType,
  Gender,
  StudentStatus,
} from '../models/student.models';
import { StudentService } from './student.service';

describe('StudentService', () => {
  let service: StudentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), StudentService],
    });
    service = TestBed.inject(StudentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('posts multipart registration payload and reports progress', () => {
    const photo = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
    const doc = new File(['pdf'], 'aadhaar.pdf', { type: 'application/pdf' });
    const events: { progress: number | null; hasResponse: boolean }[] = [];

    service
      .create(
        {
          admissionNumber: 'ADM-1',
          firstName: 'Ravi',
          gender: Gender.Male,
          dateOfBirth: '2015-01-01',
          admissionDate: '2024-06-01',
        },
        photo,
        [{ file: doc, documentType: DocumentType.AadhaarCard }],
      )
      .subscribe((event) => {
        events.push({
          progress: event.progress,
          hasResponse: !!event.response,
        });
        if (event.response) {
          expect(event.response.admissionNumber).toBe('ADM-1');
          expect(event.response.status).toBe(StudentStatus.Active);
        }
      });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.reportProgress).toBeTrue();

    req.event({
      type: HttpEventType.UploadProgress,
      loaded: 50,
      total: 100,
    });
    req.flush({
      id: '11111111-1111-1111-1111-111111111111',
      admissionNumber: 'ADM-1',
      status: StudentStatus.Active,
      createdDate: '2026-01-01T00:00:00Z',
    });

    expect(events.some((e) => e.progress === 50)).toBeTrue();
    expect(events.some((e) => e.hasResponse)).toBeTrue();
  });
});
