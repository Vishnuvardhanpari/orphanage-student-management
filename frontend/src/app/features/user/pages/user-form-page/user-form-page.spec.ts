import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthProvider, ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';
import { UserFormPage } from './user-form-page';

describe('UserFormPage', () => {
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

  let fixture: ComponentFixture<UserFormPage>;
  let component: UserFormPage;
  let userService: jasmine.SpyObj<UserService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let router: Router;

  async function setup(routeId: string | null): Promise<void> {
    userService = jasmine.createSpyObj('UserService', [
      'getById',
      'create',
      'update',
    ]);
    userService.getById.and.returnValue(of(sample));
    userService.create.and.returnValue(of(sample));
    userService.update.and.returnValue(of(sample));
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    await TestBed.configureTestingModule({
      imports: [UserFormPage],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => routeId } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  it('create mode requires password for LOCAL provider', async () => {
    await setup('new');
    expect(component.isEdit()).toBeFalse();
    component.form.patchValue({
      username: 'nurse',
      email: 'nurse@oms.local',
      password: '',
    });
    component.submit();
    expect(userService.create).not.toHaveBeenCalled();
    expect(component.form.controls.password.invalid).toBeTrue();
  });

  it('creates local user and navigates to detail', async () => {
    await setup('new');
    component.form.patchValue({
      username: 'nurse',
      email: 'nurse@oms.local',
      password: 'Password123!',
    });
    component.submit();
    expect(userService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        username: 'nurse',
        email: 'nurse@oms.local',
        authProvider: AuthProvider.Local,
        password: 'Password123!',
      }),
    );
    expect(notifications.success).toHaveBeenCalledWith('User created.');
    expect(router.navigateByUrl).toHaveBeenCalledWith(`${APP_PATHS.users}/u1`);
  });

  it('edit mode loads user and updates fields', async () => {
    await setup('u1');
    expect(component.isEdit()).toBeTrue();
    expect(userService.getById).toHaveBeenCalledWith('u1');
    expect(component.form.controls.username.value).toBe('staff1');
    expect(component.form.controls.authProvider.disabled).toBeTrue();

    component.form.patchValue({ username: 'staff1b', email: 'staff1b@oms.local' });
    component.submit();
    expect(userService.update).toHaveBeenCalledWith(
      'u1',
      jasmine.objectContaining({
        username: 'staff1b',
        email: 'staff1b@oms.local',
      }),
    );
    expect(notifications.success).toHaveBeenCalledWith('User updated.');
  });

  it('navigates to list when load fails in edit mode', async () => {
    userService = jasmine.createSpyObj('UserService', [
      'getById',
      'create',
      'update',
    ]);
    userService.getById.and.returnValue(
      throwError(() => ({ error: { message: 'Not found' } })),
    );
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [UserFormPage],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'missing' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();

    expect(notifications.error).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.users);
  });
});
