import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { UserRole } from '../enums/user-role';
import { API_PATHS } from '../constants/api-paths';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const logoutUrl = `${environment.apiBaseUrl}/${API_PATHS.auth.logout}`;
  const studentsUrl = `${environment.apiBaseUrl}/students`;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', [
      'getAccessToken',
      'refresh',
      'clearSession',
    ]);
    authService.getAccessToken.and.returnValue(null);
    authService.refresh.and.returnValue(
      of({
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: {
          id: '1',
          username: 'admin',
          email: 'admin@oms.local',
          role: UserRole.Admin,
        },
      }),
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('attaches Bearer on logout when an access token exists', () => {
    authService.getAccessToken.and.returnValue('access-token');

    http.post(logoutUrl, { refreshToken: 'r' }).subscribe();

    const req = httpMock.expectOne(logoutUrl);
    expect(req.request.headers.get('Authorization')).toBe('Bearer access-token');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(authService.refresh).not.toHaveBeenCalled();
  });

  it('sends logout without Bearer when no access token exists', () => {
    authService.getAccessToken.and.returnValue(null);

    http.post(logoutUrl, { refreshToken: 'r' }).subscribe();

    const req = httpMock.expectOne(logoutUrl);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('does not attempt refresh-on-401 for logout', () => {
    authService.getAccessToken.and.returnValue('expired-token');
    let caught = false;

    http.post(logoutUrl, {}).subscribe({ error: () => (caught = true) });

    httpMock
      .expectOne(logoutUrl)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authService.refresh).not.toHaveBeenCalled();
    expect(caught).toBeTrue();
  });

  it('attaches Bearer and refreshes on 401 for protected requests', () => {
    authService.getAccessToken.and.returnValue('stale-token');

    http.get(studentsUrl).subscribe();

    const first = httpMock.expectOne(studentsUrl);
    expect(first.request.headers.get('Authorization')).toBe('Bearer stale-token');
    first.flush(null, { status: 401, statusText: 'Unauthorized' });

    const retry = httpMock.expectOne(studentsUrl);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access');
    retry.flush([]);
    expect(authService.refresh).toHaveBeenCalled();
  });
});
