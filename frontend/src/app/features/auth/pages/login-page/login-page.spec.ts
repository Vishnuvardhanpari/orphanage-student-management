import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginPage } from './login-page';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserRole } from '../../../../core/enums/user-role';
import { APP_PATHS } from '../../../../core/constants/routes';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let page: LoginPage;
  let router: Router;
  let authService: jasmine.SpyObj<AuthService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['login', 'loginWithGoogle']);
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    page = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('navigates to dashboard after successful login', () => {
    authService.login.and.returnValue(
      of({
        accessToken: 'a',
        refreshToken: 'r',
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

    page['form'].setValue({
      username: 'admin',
      password: 'ChangeMeAdmin123!',
    });
    page['submit']();

    expect(notifications.success).toHaveBeenCalledWith(
      'Signed in successfully.',
    );
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.dashboard);
  });

  it('does not navigate when login fails', () => {
    authService.login.and.returnValue(
      throwError(() => ({ status: 401 })),
    );

    page['form'].setValue({
      username: 'admin',
      password: 'ChangeMeAdmin123!',
    });
    page['submit']();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
