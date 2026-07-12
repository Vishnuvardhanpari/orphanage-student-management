import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GridApi,
  GridReadyEvent,
  ICellRendererParams,
  ModuleRegistry,
  AllCommunityModule,
} from 'ag-grid-community';
import { catchError, finalize, firstValueFrom, of } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../components/confirm-dialog/confirm-dialog';
import {
  ResetPasswordDialog,
  ResetPasswordDialogData,
} from '../../components/reset-password-dialog/reset-password-dialog';
import { ManagedUser } from '../../models/user.models';
import { UserService } from '../../services/user.service';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [
    PageHeader,
    EmptyState,
    Button,
    AgGridAngular,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './user-list-page.html',
  styleUrl: './user-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListPage implements OnInit {
  private readonly userService = inject(UserService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private gridApi?: GridApi<ManagedUser>;

  readonly loading = signal(false);
  readonly users = signal<ManagedUser[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly paths = APP_PATHS;

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    role: [''],
    enabled: [''],
  });

  readonly columnDefs: ColDef<ManagedUser>[] = [
    { field: 'username', headerName: 'Username', flex: 1, minWidth: 140 },
    { field: 'email', headerName: 'Email', flex: 1.4, minWidth: 180 },
    { field: 'role', headerName: 'Role', width: 110 },
    {
      field: 'enabled',
      headerName: 'Status',
      width: 110,
      valueFormatter: (p) => (p.value ? 'Enabled' : 'Disabled'),
    },
    { field: 'authProvider', headerName: 'Provider', width: 130 },
    {
      field: 'lastLoginAt',
      headerName: 'Last login',
      width: 170,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleString() : '—',
    },
    {
      headerName: 'Actions',
      width: 280,
      sortable: false,
      filter: false,
      cellRenderer: (params: ICellRendererParams<ManagedUser>) => {
        const user = params.data;
        if (!user) {
          return '';
        }
        const wrap = document.createElement('div');
        wrap.className = 'user-grid-actions';
        wrap.innerHTML = `
          <button type="button" data-action="view" class="user-grid-actions__btn">View</button>
          <button type="button" data-action="edit" class="user-grid-actions__btn">Edit</button>
          <button type="button" data-action="toggle" class="user-grid-actions__btn">
            ${user.enabled ? 'Disable' : 'Enable'}
          </button>
          <button type="button" data-action="reset" class="user-grid-actions__btn">Reset PW</button>
        `;
        wrap.addEventListener('click', (event) => {
          const target = event.target as HTMLElement;
          const action = target.getAttribute('data-action');
          if (!action) {
            return;
          }
          if (action === 'view') {
            void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}`);
          } else if (action === 'edit') {
            void this.router.navigateByUrl(`${APP_PATHS.users}/${user.id}/edit`);
          } else if (action === 'toggle') {
            void this.toggleEnabled(user);
          } else if (action === 'reset') {
            void this.openResetPassword(user);
          }
        });
        return wrap;
      },
    },
  ];

  readonly defaultColDef: ColDef = {
    sortable: true,
    resizable: true,
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  onGridReady(event: GridReadyEvent<ManagedUser>): void {
    this.gridApi = event.api;
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
    this.loading.set(true);
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
        sort: 'username,asc',
      })
      .pipe(
        catchError((err) => {
          this.notifications.error(err?.error?.message || 'Failed to load users.');
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: this.pageSize(),
            number: 0,
            first: true,
            last: true,
            empty: true,
          });
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
