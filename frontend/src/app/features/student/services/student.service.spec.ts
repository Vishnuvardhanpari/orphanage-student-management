import { TestBed } from '@angular/core/testing';
import { HttpEventType, provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
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

  it('lists students with all query parameters serialized', () => {
    let total: number | undefined;
    service
      .list({
        search: 'anita',
        gender: Gender.Female,
        status: StudentStatus.Active,
        admissionYear: 2024,
        school: 'Green Valley',
        ageMin: 8,
        ageMax: 14,
        page: 2,
        size: 10,
        sort: 'firstName,asc',
      })
      .subscribe((page) => {
        total = page.totalElements;
      });

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${environment.apiBaseUrl}/${API_PATHS.students}` &&
        request.method === 'GET',
    );
    expect(req.request.params.get('search')).toBe('anita');
    expect(req.request.params.get('gender')).toBe('FEMALE');
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('admissionYear')).toBe('2024');
    expect(req.request.params.get('school')).toBe('Green Valley');
    expect(req.request.params.get('ageMin')).toBe('8');
    expect(req.request.params.get('ageMax')).toBe('14');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('sort')).toBe('firstName,asc');

    req.flush({
      content: [],
      totalElements: 42,
      totalPages: 5,
      size: 10,
      number: 2,
      first: false,
      last: false,
      empty: true,
    });
    expect(total).toBe(42);
  });

  it('lists students by exact admissionNumber', () => {
    service.list({ admissionNumber: 'ADM-100', page: 0, size: 1 }).subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${environment.apiBaseUrl}/${API_PATHS.students}` &&
        request.method === 'GET',
    );
    expect(req.request.params.get('admissionNumber')).toBe('ADM-100');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('1');
    req.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 1,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });
  });

  it('lists students omitting unset filters and applying paging defaults', () => {
    service.list().subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${environment.apiBaseUrl}/${API_PATHS.students}` &&
        request.method === 'GET',
    );
    expect(req.request.params.has('search')).toBeFalse();
    expect(req.request.params.has('gender')).toBeFalse();
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('admissionYear')).toBeFalse();
    expect(req.request.params.has('school')).toBeFalse();
    expect(req.request.params.has('ageMin')).toBeFalse();
    expect(req.request.params.has('ageMax')).toBeFalse();
    expect(req.request.params.has('sort')).toBeFalse();
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');

    req.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });
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

  it('marks blob requests to skip the global error toast', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const documentId = '22222222-2222-2222-2222-222222222222';

    service.fetchPhoto(studentId).subscribe();
    const photoReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/photo`,
    );
    expect(photoReq.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    photoReq.flush(new Blob(['img']));

    service.downloadDocument(studentId, documentId).subscribe();
    const downloadReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents/${documentId}/download`,
    );
    expect(downloadReq.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    downloadReq.flush(new Blob(['pdf']));
  });

  it('deletes the profile photo via DELETE', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let completed = false;
    service.deletePhoto(studentId).subscribe(() => {
      completed = true;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/photo`,
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('deletes a document via DELETE', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const documentId = '22222222-2222-2222-2222-222222222222';
    let completed = false;
    service.deleteDocument(studentId, documentId).subscribe(() => {
      completed = true;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents/${documentId}`,
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('soft-deletes a student via DELETE with no body when no exit details are given', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let completed = false;
    service.softDelete(studentId).subscribe(() => {
      completed = true;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}`,
    );
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toBeNull();
    req.flush(null);
    expect(completed).toBeTrue();
  });

  // Regression for QA BUG-005: optional exit details must be sent as a JSON
  // body so the backend can record them in the same request as the archive.
  it('soft-deletes a student with exit details in the request body', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let completed = false;
    service
      .softDelete(studentId, {
        exitDate: '2026-01-10',
        exitReason: 'Family relocated',
        exitRemarks: 'Handed over to guardian',
      })
      .subscribe(() => {
        completed = true;
      });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}`,
    );
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toEqual({
      exitDate: '2026-01-10',
      exitReason: 'Family relocated',
      exitRemarks: 'Handed over to guardian',
    });
    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('sends no body when exit details are provided but every field is blank', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    service.softDelete(studentId, {}).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}`,
    );
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('restores a student via PATCH', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let status: StudentStatus | undefined;
    service.restore(studentId).subscribe((detail) => {
      status = detail.status;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/restore`,
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({
      id: studentId,
      admissionNumber: 'ADM-1',
      firstName: 'Anita',
      lastName: null,
      gender: Gender.Female,
      dateOfBirth: '2014-03-15',
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
    expect(status).toBe(StudentStatus.Active);
  });

  it('lists inactive students with paging defaults', () => {
    let total = -1;
    service.listInactive().subscribe((page) => {
      total = page.totalElements;
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/${API_PATHS.students}/inactive` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20',
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });
    expect(total).toBe(0);
  });

  it('updates student fields via PUT', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    let firstName = '';
    service
      .update(studentId, {
        firstName: 'Anita',
        gender: Gender.Female,
        dateOfBirth: '2014-03-15',
        admissionDate: '2024-06-01',
      })
      .subscribe((detail) => {
        firstName = detail.firstName;
      });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}`,
    );
    expect(req.request.method).toBe('PUT');
    req.flush({
      id: studentId,
      admissionNumber: 'ADM-1',
      firstName: 'Anita',
      lastName: null,
      gender: Gender.Female,
      dateOfBirth: '2014-03-15',
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
      updatedDate: '2026-01-02T00:00:00Z',
    });
    expect(firstName).toBe('Anita');
  });

  it('replaces photo via multipart PUT', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const photo = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
    let completed = false;
    service.replacePhoto(studentId, photo).subscribe(() => {
      completed = true;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/photo`,
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(null);
    expect(completed).toBeTrue();
  });

  it('adds documents via multipart POST', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const doc = new File(['pdf'], 'birth.pdf', { type: 'application/pdf' });
    let count = 0;
    service
      .addDocuments(studentId, [{ file: doc, documentType: DocumentType.BirthCertificate }])
      .subscribe((docs) => {
        count = docs.length;
      });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents`,
    );
    expect(req.request.method).toBe('POST');
    req.flush([
      {
        id: '22222222-2222-2222-2222-222222222222',
        documentType: DocumentType.BirthCertificate,
        originalFileName: 'birth.pdf',
        contentType: 'application/pdf',
        fileSize: 4,
        uploadedDate: '2026-01-01T00:00:00Z',
      },
    ]);
    expect(count).toBe(1);
  });

  it('replaces a document via multipart PUT', () => {
    const studentId = '11111111-1111-1111-1111-111111111111';
    const documentId = '22222222-2222-2222-2222-222222222222';
    const file = new File(['pdf'], 'identity.pdf', { type: 'application/pdf' });
    let name = '';
    service.replaceDocument(studentId, documentId, file, DocumentType.IdentityProof).subscribe((doc) => {
      name = doc.originalFileName;
    });

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/${API_PATHS.students}/${studentId}/documents/${documentId}`,
    );
    expect(req.request.method).toBe('PUT');
    req.flush({
      id: documentId,
      documentType: DocumentType.IdentityProof,
      originalFileName: 'identity.pdf',
      contentType: 'application/pdf',
      fileSize: 8,
      uploadedDate: '2026-01-02T00:00:00Z',
    });
    expect(name).toBe('identity.pdf');
  });
});
