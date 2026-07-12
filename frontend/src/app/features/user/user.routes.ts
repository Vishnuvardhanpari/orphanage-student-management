import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/enums/user-role';
import { UserListPage } from './pages/user-list-page/user-list-page';
import { UserFormPage } from './pages/user-form-page/user-form-page';
import { UserDetailPage } from './pages/user-detail-page/user-detail-page';

export const USER_ROUTES: Routes = [
  {
    path: '',
    component: UserListPage,
    title: 'Users',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
  {
    path: 'new',
    component: UserFormPage,
    title: 'Add user',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
  {
    path: ':id/edit',
    component: UserFormPage,
    title: 'Edit user',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
  {
    path: ':id',
    component: UserDetailPage,
    title: 'User details',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
];
