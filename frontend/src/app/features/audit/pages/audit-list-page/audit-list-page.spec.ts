import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuditLog } from '../../models/audit.models';
import { AuditService } from '../../services/audit.service';
import { AuditListPage } from './audit-list-page';

describe('AuditListPage', () => {
  let fixture: ComponentFixture<AuditListPage>;
  let component: AuditListPage;
  let auditService: jasmine.SpyObj<AuditService>;
  let notifications: jasmine.SpyObj<NotificationService>;

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

  beforeEach(async () => {
    auditService = jasmine.createSpyObj('AuditService', ['list', 'getById']);
    auditService.list.and.returnValue(of(pageResponse));
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
      'info',
    ]);

    await TestBed.configureTestingModule({
      imports: [AuditListPage],
      providers: [
        provideRouter([]),
        { provide: AuditService, useValue: auditService },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads audit logs on init', () => {
    expect(auditService.list).toHaveBeenCalled();
    expect(component.logs().length).toBe(1);
    expect(component.totalElements()).toBe(1);
    expect(component.loadFailed()).toBeFalse();
  });

  it('applies filters and resets page', () => {
    component.page.set(2);
    component.filterForm.patchValue({
      module: 'AUTH',
      action: 'LOGIN',
    });
    component.applyFilters();
    expect(component.page()).toBe(0);
    expect(auditService.list).toHaveBeenCalled();
  });

  it('shows loadFailed and does not imply empty data when list fails', () => {
    auditService.list.and.returnValue(throwError(() => ({ status: 500 })));
    component.loadLogs();
    expect(component.loadFailed()).toBeTrue();
    expect(component.logs()).toEqual([]);
    expect(notifications.error).not.toHaveBeenCalled();
  });

  it('blocks Apply when From is after To', () => {
    const callsBefore = auditService.list.calls.count();
    component.filterForm.patchValue({
      from: '2026-07-18',
      to: '2026-07-10',
    });
    component.applyFilters();
    expect(notifications.error).toHaveBeenCalledWith(
      'From date must be on or before To date.',
    );
    expect(auditService.list.calls.count()).toBe(callsBefore);
  });

  it('converts date filters using local timezone day bounds', () => {
    component.filterForm.patchValue({
      from: '2026-07-18',
      to: '2026-07-18',
    });
    component.applyFilters();

    const args = auditService.list.calls.mostRecent().args[0];
    const expectedFrom = new Date(2026, 6, 18, 0, 0, 0, 0).toISOString();
    const expectedTo = new Date(2026, 6, 18, 23, 59, 59, 999).toISOString();
    expect(args?.from).toBe(expectedFrom);
    expect(args?.to).toBe(expectedTo);
    expect(args?.from?.endsWith('Z')).toBeTrue();
  });
});
