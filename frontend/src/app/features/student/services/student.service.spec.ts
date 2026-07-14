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

  it('gets student profile by id', () => {
    let detail: { admissionNumber: string } | undefined;
    service.getById('11111111-1111-1111-1111-111111111111').subscribe((value) => {
      detail = value;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/11111111-1111-1111-1111-111111111111`,
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: '11111111-1111-1111-1111-111111111111',
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
    });

    expect(detail?.admissionNumber).toBe('ADM-1');
  });

  it('lists documents and downloads with filename from header', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const documentId = '22222222-2222-2222-2222-222222222222';

    service.listDocuments(studentId).subscribe((docs) => {
      expect(docs.length).toBe(1);
      expect(docs[0].originalFileName).toBe('aadhaar.pdf');
    });
    const listReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents`,
    );
    listReq.flush([
      {
        id: documentId,
        documentType: DocumentType.AadhaarCard,
        originalFileName: 'aadhaar.pdf',
        contentType: 'application/pdf',
        fileSize: 12,
        uploadedDate: '2026-01-01T00:00:00Z',
      },
    ]);

    let fileName: string | null | undefined;
    service.downloadDocument(studentId, documentId).subscribe((file) => {
      fileName = file.fileName;
      expect(file.blob.size).toBeGreaterThan(0);
    });
    const downloadReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents/${documentId}/download`,
    );
    expect(downloadReq.request.responseType).toBe('blob');
    downloadReq.flush(new Blob(['pdf']), {
      headers: {
        'Content-Disposition': 'attachment; filename="aadhaar.pdf"',
        'Content-Type': 'application/pdf',
      },
    });
    expect(fileName).toBe('aadhaar.pdf');
  });

  it('returns null fileName when Content-Disposition is missing', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const documentId = '22222222-2222-2222-2222-222222222222';

    let fileName: string | null | undefined = undefined;
    service.downloadDocument(studentId, documentId).subscribe((file) => {
      fileName = file.fileName;
    });
    const downloadReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents/${documentId}/download`,
    );
    downloadReq.flush(new Blob(['pdf']), {
      headers: { 'Content-Type': 'application/pdf' },
    });
    expect(fileName).toBeNull();
  });

  it('fetches profile photo as a blob', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let size = 0;
    service.fetchPhoto(studentId).subscribe((blob) => {
      size = blob.size;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/photo`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['img'], { type: 'image/jpeg' }));
    expect(size).toBeGreaterThan(0);
  });
});
