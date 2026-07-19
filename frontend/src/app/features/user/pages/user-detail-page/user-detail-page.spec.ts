import { Dialog } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { UserRole } from '../../../../core/enums/user-role';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthService } from '../../../auth/services/auth.service';
import { AuthProvider, ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';
import { UserDetailPage } from './user-detail-page';

describe('UserDetailPage', () => {
  const sample: ManagedUser = {
    id: 'u1',
    username: 'staff1',
    email: 'staff1@oms.local',
    role: 'STAFF',
    enabled: true,
    authProvider: AuthProvider.Local,
    accountNonLocked: true,
    lastLoginAt: null,
    createdDate: '2026-07-01T00:00:00Z',
    updatedDate: '2026-07-01T00:00:00Z',
  };

  let fixture: ComponentFixture<UserDetailPage>;
  let component: UserDetailPage;
  let userService: jasmine.SpyObj<UserService>;
  let authService: jasmine.SpyObj<AuthService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<Dialog>;
  let router: Router;

  async function setup(routeId: string | null = 'u1'): Promise<void> {
    userService = jasmine.createSpyObj('UserService', [
      'getById',
      'enable',
      'disable',
      'resetPassword',
    ]);
    userService.getById.and.returnValue(of(sample));
    authService = jasmine.createSpyObj('AuthService', [], {
      currentUser: jasmine.createSpy('currentUser').and.returnValue({
        id: 'admin-1',
        username: 'admin',
        email: 'admin@oms.local',
        role: UserRole.Admin,
      }),
    });
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);
    dialog = jasmine.createSpyObj('Dialog', ['open']);

    await TestBed.configureTestingModule({
      imports: [UserDetailPage],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notifications },
        { provide: Dialog, useValue: dialog },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => routeId } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  it('loads user by route id', async () => {
    await setup();
    expect(userService.getById).toHaveBeenCalledWith('u1');
    expect(component.user()?.username).toBe('staff1');
    expect(component.isSelf()).toBeFalse();
  });

  it('navigates to list when id missing', async () => {
    await setup(null);
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.users);
  });

  it('navigates to list when load fails', async () => {
    userService = jasmine.createSpyObj('UserService', [
      'getById',
      'enable',
      'disable',
      'resetPassword',
    ]);
    userService.getById.and.returnValue(
      throwError(() => ({ error: { message: 'Not found' } })),
    );
    authService = jasmine.createSpyObj('AuthService', [], {
      currentUser: jasmine.createSpy('currentUser').and.returnValue(null),
    });
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);
    dialog = jasmine.createSpyObj('Dialog', ['open']);

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [UserDetailPage],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notifications },
        { provide: Dialog, useValue: dialog },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'missing' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();

    expect(notifications.error).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.users);
  });

  it('disables user after confirm', async () => {
    await setup();
    dialog.open.and.returnValue({ closed: of(true) } as never);
    const disabled = { ...sample, enabled: false };
    userService.disable.and.returnValue(of(disabled));

    await component.toggleEnabled();

    expect(userService.disable).toHaveBeenCalledWith('u1');
    expect(component.user()?.enabled).toBeFalse();
    expect(notifications.success).toHaveBeenCalledWith('User disabled.');
  });

  it('resets password after dialog confirms', async () => {
    await setup();
    dialog.open.and.returnValue({ closed: of('Password123!') } as never);
    userService.resetPassword.and.returnValue(of(sample));

    await component.resetPassword();

    expect(userService.resetPassword).toHaveBeenCalledWith('u1', {
      newPassword: 'Password123!',
    });
    expect(notifications.success).toHaveBeenCalledWith('Password reset successfully.');
  });
});
