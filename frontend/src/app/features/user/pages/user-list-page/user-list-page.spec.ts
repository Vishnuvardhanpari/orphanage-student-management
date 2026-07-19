import { Dialog } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserRole } from '../../../../core/enums/user-role';
import { AuthService } from '../../../auth/services/auth.service';
import { AuthProvider, ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';
import { UserListPage } from './user-list-page';

describe('UserListPage', () => {
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

  const pageResponse = {
    content: [sample],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  let fixture: ComponentFixture<UserListPage>;
  let component: UserListPage;
  let userService: jasmine.SpyObj<UserService>;
  let authService: jasmine.SpyObj<AuthService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<Dialog>;

  beforeEach(async () => {
    userService = jasmine.createSpyObj('UserService', [
      'list',
      'enable',
      'disable',
      'resetPassword',
    ]);
    userService.list.and.returnValue(of(pageResponse));
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
      imports: [UserListPage],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notifications },
        { provide: Dialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads users on init', () => {
    expect(userService.list).toHaveBeenCalled();
    expect(component.users().length).toBe(1);
    expect(component.loadFailed()).toBeFalse();
    expect(component.gridContext.currentUserId).toBe('admin-1');
  });

  it('applies filters and resets page', () => {
    component.page.set(2);
    component.filterForm.patchValue({ search: 'staff', role: 'STAFF', enabled: 'true' });
    component.applyFilters();
    expect(component.page()).toBe(0);
    expect(userService.list).toHaveBeenCalledWith(
      jasmine.objectContaining({
        search: 'staff',
        role: 'STAFF',
        enabled: true,
        page: 0,
      }),
    );
  });

  it('clears filters', () => {
    component.filterForm.patchValue({ search: 'x', role: 'ADMIN', enabled: 'false' });
    component.clearFilters();
    expect(component.filterForm.getRawValue()).toEqual({
      search: '',
      role: '',
      enabled: '',
    });
    expect(userService.list).toHaveBeenCalled();
  });

  it('sets loadFailed without toast when list fails', () => {
    userService.list.and.returnValue(throwError(() => ({ status: 500 })));
    component.loadUsers();
    expect(component.loadFailed()).toBeTrue();
    expect(component.users()).toEqual([]);
    expect(notifications.error).not.toHaveBeenCalled();
  });

  it('toggles enabled after confirm', async () => {
    dialog.open.and.returnValue({ closed: of(true) } as never);
    userService.disable.and.returnValue(of(sample));

    await component['toggleEnabled'](sample);

    expect(userService.disable).toHaveBeenCalledWith('u1');
    expect(notifications.success).toHaveBeenCalledWith('User disabled.');
  });

  it('skips toggle for self', async () => {
    (authService.currentUser as jasmine.Spy).and.returnValue({
      id: 'u1',
      username: 'staff1',
      email: 'staff1@oms.local',
      role: UserRole.Staff,
    });

    await component['toggleEnabled'](sample);

    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('resets password after dialog confirms', async () => {
    dialog.open.and.returnValue({ closed: of('Password123!') } as never);
    userService.resetPassword.and.returnValue(of(sample));

    await component['openResetPassword'](sample);

    expect(userService.resetPassword).toHaveBeenCalledWith('u1', {
      newPassword: 'Password123!',
    });
    expect(notifications.success).toHaveBeenCalledWith('Password reset successfully.');
  });
});
