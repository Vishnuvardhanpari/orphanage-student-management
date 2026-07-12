import { Routes } from '@angular/router';
import { UserListPage } from './pages/user-list-page/user-list-page';
import { roleGuard } from '../../core/guards/role.guard';
import { UserRole } from '../../core/enums/user-role';

export const USER_ROUTES: Routes = [
  {
    path: '',
    component: UserListPage,
    title: 'Users',
    canActivate: [roleGuard],
    data: { roles: [UserRole.Admin] },
  },
];
