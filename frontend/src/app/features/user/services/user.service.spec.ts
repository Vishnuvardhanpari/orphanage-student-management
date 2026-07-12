import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { UserService } from './user.service';
import { AuthProvider, ManagedUser } from '../models/user.models';
import { UserRole } from '../../../core/enums/user-role';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const sampleUser: ManagedUser = {
    id: 'u1',
    username: 'staff1',
    email: 'staff1@oms.local',
    role: UserRole.Staff,
    enabled: true,
    authProvider: AuthProvider.Local,
    accountNonLocked: true,
    lastLoginAt: null,
    createdDate: '2026-01-01T00:00:00Z',
    updatedDate: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), UserService],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists users with query params', () => {
    service.list({ search: 'staff', role: 'STAFF', page: 1, size: 10 }).subscribe((page) => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].username).toBe('staff1');
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/v1/users' &&
        r.params.get('search') === 'staff' &&
        r.params.get('role') === 'STAFF' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [sampleUser],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 1,
      first: false,
      last: true,
      empty: false,
    });
  });

  it('creates a user', () => {
    service
      .create({
        username: 'staff1',
        email: 'staff1@oms.local',
        role: UserRole.Staff,
        authProvider: AuthProvider.Local,
        password: 'Password123!',
      })
      .subscribe((user) => {
        expect(user.id).toBe('u1');
      });

    const req = httpMock.expectOne('/api/v1/users');
    expect(req.request.method).toBe('POST');
    req.flush(sampleUser);
  });

  it('disables a user', () => {
    service.disable('u1').subscribe((user) => {
      expect(user.enabled).toBeFalse();
    });

    const req = httpMock.expectOne('/api/v1/users/u1/disable');
    expect(req.request.method).toBe('POST');
    req.flush({ ...sampleUser, enabled: false });
  });

  it('resets a password', () => {
    service.resetPassword('u1', { newPassword: 'NewPass123!' }).subscribe((user) => {
      expect(user.id).toBe('u1');
    });

    const req = httpMock.expectOne('/api/v1/users/u1/reset-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ newPassword: 'NewPass123!' });
    req.flush(sampleUser);
  });
});
