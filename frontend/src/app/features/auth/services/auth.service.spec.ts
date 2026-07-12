import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { UserRole } from '../../../core/enums/user-role';
import { STORAGE_KEYS } from '../../../core/constants/storage-keys';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        AuthService,
        TokenStorageService,
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('persists session on login', () => {
    service.login({ username: 'admin', password: 'ChangeMeAdmin123!' }).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: '1',
        username: 'admin',
        email: 'admin@oms.local',
        role: UserRole.Admin,
      },
    });

    expect(tokenStorage.getAccessToken()).toBe('access');
    expect(tokenStorage.getRefreshToken()).toBe('refresh');
    expect(service.currentUser()?.username).toBe('admin');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('clears session on logout finalize', () => {
    tokenStorage.saveSession('access', 'refresh', {
      id: '1',
      username: 'admin',
      email: 'admin@oms.local',
      role: UserRole.Admin,
    });

    service.logout(false).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/logout');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(sessionStorage.getItem(STORAGE_KEYS.accessToken)).toBeNull();
    expect(service.currentUser()).toBeNull();
  });
});
