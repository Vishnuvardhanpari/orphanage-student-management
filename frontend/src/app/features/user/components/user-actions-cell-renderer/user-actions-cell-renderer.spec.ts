import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthProvider, ManagedUser } from '../../models/user.models';
import {
  UserActionsCellContext,
  UserActionsCellRenderer,
} from './user-actions-cell-renderer';

describe('UserActionsCellRenderer', () => {
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

  let fixture: ComponentFixture<UserActionsCellRenderer>;
  let component: UserActionsCellRenderer;
  let context: jasmine.SpyObj<UserActionsCellContext>;

  beforeEach(async () => {
    context = jasmine.createSpyObj('UserActionsCellContext', [
      'onView',
      'onEdit',
      'onToggle',
      'onReset',
    ]);
    context.currentUserId = 'admin-1';

    await TestBed.configureTestingModule({
      imports: [UserActionsCellRenderer],
    }).compileComponents();

    fixture = TestBed.createComponent(UserActionsCellRenderer);
    component = fixture.componentInstance;
    component.agInit({
      data: sample,
      context,
    } as never);
    fixture.detectChanges();
  });

  it('shows toggle for other users and invokes callbacks', () => {
    expect(component.isSelf()).toBeFalse();
    component.view();
    component.edit();
    component.toggle();
    component.reset();
    expect(context.onView).toHaveBeenCalledWith(sample);
    expect(context.onEdit).toHaveBeenCalledWith(sample);
    expect(context.onToggle).toHaveBeenCalledWith(sample);
    expect(context.onReset).toHaveBeenCalledWith(sample);
  });

  it('hides toggle for self and skips toggle callback', () => {
    context.currentUserId = 'u1';
    component.refresh({
      data: sample,
      context,
    } as never);
    fixture.detectChanges();

    expect(component.isSelf()).toBeTrue();
    component.toggle();
    expect(context.onToggle).not.toHaveBeenCalled();
  });
});
