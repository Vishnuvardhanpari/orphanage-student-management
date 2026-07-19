import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuditLog } from '../../models/audit.models';
import { AuditService } from '../../services/audit.service';
import { AuditDetailPage } from './audit-detail-page';

describe('AuditDetailPage', () => {
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

  let fixture: ComponentFixture<AuditDetailPage>;
  let component: AuditDetailPage;
  let auditService: jasmine.SpyObj<AuditService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let router: Router;

  async function setup(routeId: string | null = 'a1'): Promise<void> {
    auditService = jasmine.createSpyObj('AuditService', ['getById']);
    auditService.getById.and.returnValue(of(sample));
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    await TestBed.configureTestingModule({
      imports: [AuditDetailPage],
      providers: [
        provideRouter([]),
        { provide: AuditService, useValue: auditService },
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => routeId } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditDetailPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  }

  it('loads audit log by id', async () => {
    await setup();
    expect(auditService.getById).toHaveBeenCalledWith('a1');
    expect(component.log()?.action).toBe('LOGIN');
  });

  it('navigates to list when id missing', async () => {
    await setup(null);
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.audit);
  });

  it('toasts and navigates when load fails', async () => {
    auditService = jasmine.createSpyObj('AuditService', ['getById']);
    auditService.getById.and.returnValue(
      throwError(() => ({ error: { message: 'Missing' } })),
    );
    notifications = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
    ]);

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AuditDetailPage],
      providers: [
        provideRouter([]),
        { provide: AuditService, useValue: auditService },
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'missing' } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditDetailPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();

    expect(notifications.error).toHaveBeenCalledWith('Missing');
    expect(router.navigateByUrl).toHaveBeenCalledWith(APP_PATHS.audit);
  });
});
