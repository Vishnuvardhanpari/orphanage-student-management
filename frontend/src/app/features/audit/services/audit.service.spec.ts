import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import { AuditService } from './audit.service';
import { AuditLog } from '../models/audit.models';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;

  const sample: AuditLog = {
    id: 'a1',
    module: 'AUTH',
    action: 'LOGIN',
    entityId: 'u1',
    description: 'User logged in via password',
    username: 'admin',
    ipAddress: '127.0.0.1',
    createdDate: '2026-07-18T12:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AuditService],
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists audit logs with query params and skips error toast', () => {
    service
      .list({
        search: 'login',
        module: 'AUTH',
        action: 'LOGIN',
        username: 'admin',
        page: 0,
        size: 20,
        sort: 'createdDate,desc',
      })
      .subscribe((page) => {
        expect(page.content.length).toBe(1);
        expect(page.content[0].username).toBe('admin');
      });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/v1/audit' &&
        r.params.get('search') === 'login' &&
        r.params.get('module') === 'AUTH' &&
        r.params.get('action') === 'LOGIN' &&
        r.params.get('username') === 'admin' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20' &&
        r.params.get('sort') === 'createdDate,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeTrue();
    req.flush({
      content: [sample],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });
  });

  it('gets audit log by id', () => {
    service.getById('a1').subscribe((log) => {
      expect(log.id).toBe('a1');
      expect(log.action).toBe('LOGIN');
    });

    const req = httpMock.expectOne('/api/v1/audit/a1');
    expect(req.request.method).toBe('GET');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBeFalse();
    req.flush(sample);
  });
});
