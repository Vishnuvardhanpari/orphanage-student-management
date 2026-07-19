import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  AllCommunityModule,
  SortChangedEvent,
} from 'ag-grid-community';
import { catchError, finalize, firstValueFrom, of } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthService } from '../../../auth/services/auth.service';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Field } from '../../../../shared/components/field/field';
import { FilterPanel } from '../../../../shared/components/filter-panel/filter-panel';
import { Input } from '../../../../shared/components/input/input';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { PaginationBar } from '../../../../shared/components/pagination-bar/pagination-bar';
import { Select } from '../../../../shared/components/select/select';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog';
import {
  ResetPasswordDialog,
  ResetPasswordDialogData,
} from '../../components/reset-password-dialog/reset-password-dialog';
import {
  UserActionsCellContext,
  UserActionsCellRenderer,
} from '../../components/user-actions-cell-renderer/user-actions-cell-renderer';
import { ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';
import { emptyPage } from '../../../../shared/models/page.models';

ModuleRegistry.registerModules([AllCommunityModule]);

/** No-op comparator so AG Grid does not reorder the current page client-side. */
const serverSortComparator = (): number => 0;

const SORT_FIELD_MAP: Record<string, string> = {
  username: 'username',
  email: 'email',
  role: 'role.name',
  enabled: 'enabled',
  authProvider: 'authProvider',
  lastLoginAt: 'lastLoginAt',
};

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [
    PageHeader,
    EmptyState,
    Button,
    AgGridAngular,
    ReactiveFormsModule,
    Field,
    FilterPanel,
    Input,
    Select,
    PaginationBar,
  ],
  templateUrl: './user-list-page.html',
  styleUrl: './user-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListPage implements OnInit {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private gridApi?: GridApi<ManagedUser>;

  readonly loading = signal(false);
  readonly loadFailed = signal(false);
  readonly users = signal<ManagedUser[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal('username,asc');
  readonly paths = APP_PATHS;

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    role: [''],
    enabled: [''],
  });

  readonly gridContext: UserActionsCellContext = {
    currentUserId: null,
    onView: (user) => {
      void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}`);
    },
    onEdit: (user) => {
      void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}/edit`);
    },
    onToggle: (user) => {
      void this.toggleEnabled(user);
    },
    onReset: (user) => {
      void this.openResetPassword(user);
    },
  };

  readonly columnDefs: ColDef<ManagedUser>[] = [
    {
      field: 'username',
      headerName: 'Username',
      flex: 1,
      minWidth: 140,
      comparator: serverSortComparator,
    },
    {
      field: 'email',
      headerName: 'Email',
      flex: 1.4,
      minWidth: 180,
      comparator: serverSortComparator,
    },
    {
      field: 'role',
      headerName: 'Role',
      width: 110,
      comparator: serverSortComparator,
    },
    {
      field: 'enabled',
      headerName: 'Status',
      width: 110,
      valueFormatter: (p) => (p.value ? 'Enabled' : 'Disabled'),
      comparator: serverSortComparator,
    },
    {
      field: 'authProvider',
      headerName: 'Provider',
      width: 130,
      comparator: serverSortComparator,
    },
    {
      field: 'lastLoginAt',
      headerName: 'Last login',
      width: 170,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleString() : '—',
      comparator: serverSortComparator,
    },
    {
      headerName: 'Actions',
      width: 320,
      sortable: false,
      filter: false,
      cellRenderer: UserActionsCellRenderer,
    },
  ];

  readonly defaultColDef: ColDef = {
    sortable: true,
    resizable: true,
    unSortIcon: true,
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  /** Refresh current-user id on the grid context before each render/load. */
  private syncGridContext(): void {
    this.gridContext.currentUserId = this.authService.currentUser()?.id ?? null;
  }

  onGridReady(event: GridReadyEvent<ManagedUser>): void {
    this.gridApi = event.api;
  }

  onSortChanged(event: SortChangedEvent<ManagedUser>): void {
    const sorted = event.api
      .getColumnState()
      .find((col) => col.sort != null);
    let nextSort = 'username,asc';
    if (sorted?.colId && sorted.sort && SORT_FIELD_MAP[sorted.colId]) {
      nextSort = `${SORT_FIELD_MAP[sorted.colId]},${sorted.sort}`;
    }
    if (nextSort === this.sort()) {
      return;
    }
    this.sort.set(nextSort);
    this.page.set(0);
    this.loadUsers();
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadUsers();
  }

  clearFilters(): void {
    this.filterForm.reset({ search: '', role: '', enabled: '' });
    this.page.set(0);
    this.loadUsers();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadUsers();
    }
  }

  nextPage(): void {
    const maxPage = Math.max(0, Math.ceil(this.totalElements() / this.pageSize()) - 1);
    if (this.page() < maxPage) {
      this.page.update((p) => p + 1);
      this.loadUsers();
    }
  }

  loadUsers(): void {
    this.syncGridContext();
    this.loading.set(true);
    this.loadFailed.set(false);
    const filters = this.filterForm.getRawValue();
    const enabled =
      filters.enabled === '' ? undefined : filters.enabled === 'true';

    this.userService
      .list({
        search: filters.search.trim() || undefined,
        role: filters.role || undefined,
        enabled,
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .pipe(
        catchError(() => {
          this.loadFailed.set(true);
          return of(emptyPage<ManagedUser>(this.pageSize()));
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((page) => {
        this.users.set(page.content);
        this.totalElements.set(page.totalElements);
        this.gridApi?.setGridOption('rowData', page.content);
      });
  }

  private async toggleEnabled(user: ManagedUser): Promise<void> {
    if (user.id === this.authService.currentUser()?.id) {
      return;
    }

    const enabling = !user.enabled;
    const ref = this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialog, {
      data: {
        title: enabling ? 'Enable user' : 'Disable user',
        message: enabling
          ? `Enable account for ${user.username}?`
          : `Disable account for ${user.username}? Active sessions will be revoked.`,
        confirmLabel: enabling ? 'Enable' : 'Disable',
        danger: !enabling,
      },
    });
    const confirmed = await firstValueFrom(ref.closed);
    if (!confirmed) {
      return;
    }

    const request$ = enabling
      ? this.userService.enable(user.id)
      : this.userService.disable(user.id);

    request$.subscribe({
      next: () => {
        this.notifications.success(
          enabling ? 'User enabled.' : 'User disabled.',
        );
        this.loadUsers();
      },
      error: (err) => {
        this.notifications.error(err?.error?.message || 'Action failed.');
      },
    });
  }

  private async openResetPassword(user: ManagedUser): Promise<void> {
    const ref = this.dialog.open<string | null, ResetPasswordDialogData>(
      ResetPasswordDialog,
      { data: { username: user.username } },
    );
    const newPassword = await firstValueFrom(ref.closed);
    if (!newPassword) {
      return;
    }

    this.userService.resetPassword(user.id, { newPassword }).subscribe({
      next: () => {
        this.notifications.success('Password reset successfully.');
        this.loadUsers();
      },
      error: (err) => {
        this.notifications.error(err?.error?.message || 'Password reset failed.');
      },
    });
  }
}
