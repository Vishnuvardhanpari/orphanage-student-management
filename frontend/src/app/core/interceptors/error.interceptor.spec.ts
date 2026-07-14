import {
  HttpClient,
  HttpContext,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificationService } from '../services/notification.service';
import { SKIP_ERROR_TOAST, errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let notifications: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    notifications = jasmine.createSpyObj('NotificationService', ['error']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: notifications },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('toasts the API error message exactly once and rethrows', () => {
    let caught: unknown;
    http.get('/api/v1/students/x').subscribe({ error: (err) => (caught = err) });

    httpMock
      .expectOne('/api/v1/students/x')
      .flush({ message: 'Student not found.' }, { status: 404, statusText: 'Not Found' });

    expect(notifications.error).toHaveBeenCalledTimes(1);
    expect(notifications.error).toHaveBeenCalledWith('Student not found.');
    expect(caught).toBeTruthy();
  });

  it('skips the toast when the request opts out via SKIP_ERROR_TOAST', () => {
    let caught: unknown;
    http
      .get('/api/v1/students/x/photo', {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .subscribe({ error: (err) => (caught = err) });

    httpMock
      .expectOne('/api/v1/students/x/photo')
      .flush(null, { status: 404, statusText: 'Not Found' });

    expect(notifications.error).not.toHaveBeenCalled();
    expect(caught).toBeTruthy();
  });
});
