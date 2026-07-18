import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/enums/user-role';
import { AuditListPage } from './pages/audit-list-page/audit-list-page';
import { AuditDetailPage } from './pages/audit-detail-page/audit-detail-page';

export const AUDIT_ROUTES: Routes = [
  {
    path: '',
    component: AuditListPage,
    title: 'Audit Logs',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
  {
    path: ':id',
    component: AuditDetailPage,
    title: 'Audit event',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
];
