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
  AllCommunityModule,
  ColDef,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  SortChangedEvent,
} from 'ag-grid-community';
import { catchError, finalize, of } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Field } from '../../../../shared/components/field/field';
import { FilterPanel } from '../../../../shared/components/filter-panel/filter-panel';
import { Input } from '../../../../shared/components/input/input';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { PaginationBar } from '../../../../shared/components/pagination-bar/pagination-bar';
import { Select } from '../../../../shared/components/select/select';
import { emptyPage } from '../../../../shared/models/page.models';
import {
  AUDIT_ACTIONS,
  AUDIT_MODULES,
  AuditLog,
} from '../../models/audit.models';
import { AuditService } from '../../services/audit.service';

ModuleRegistry.registerModules([AllCommunityModule]);

const serverSortComparator = (): number => 0;

const SORT_FIELD_MAP: Record<string, string> = {
  createdDate: 'createdDate',
  username: 'username',
  module: 'module',
  action: 'action',
};

@Component({
  selector: 'app-audit-list-page',
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
  templateUrl: './audit-list-page.html',
  styleUrl: './audit-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditListPage implements OnInit {
  private readonly auditService = inject(AuditService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private gridApi?: GridApi<AuditLog>;

  readonly loading = signal(false);
  /** True when the last list request failed; drives the error state + Retry. */
  readonly loadFailed = signal(false);
  readonly logs = signal<AuditLog[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal('createdDate,desc');
  readonly modules = AUDIT_MODULES;
  readonly actions = AUDIT_ACTIONS;

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    module: [''],
    action: [''],
    username: [''],
    from: [''],
    to: [''],
  });

  readonly columnDefs: ColDef<AuditLog>[] = [
    {
      field: 'createdDate',
      headerName: 'When',
      width: 180,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleString() : '—',
      comparator: serverSortComparator,
    },
    {
      field: 'username',
      headerName: 'User',
      width: 130,
      comparator: serverSortComparator,
    },
    {
      field: 'module',
      headerName: 'Module',
      width: 120,
      comparator: serverSortComparator,
    },
    {
      field: 'action',
      headerName: 'Action',
      width: 120,
      comparator: serverSortComparator,
    },
    {
      field: 'description',
      headerName: 'Description',
      flex: 1.6,
      minWidth: 220,
      sortable: false,
    },
    {
      field: 'ipAddress',
      headerName: 'IP',
      width: 130,
      valueFormatter: (p) => p.value || '—',
      sortable: false,
    },
  ];

  readonly defaultColDef: ColDef = {
    sortable: true,
    resizable: true,
    unSortIcon: true,
  };

  ngOnInit(): void {
    this.loadLogs();
  }

  onGridReady(event: GridReadyEvent<AuditLog>): void {
    this.gridApi = event.api;
  }

  onSortChanged(event: SortChangedEvent<AuditLog>): void {
    const sorted = event.api.getColumnState().find((col) => col.sort != null);
    let nextSort = 'createdDate,desc';
    if (sorted?.colId && sorted.sort && SORT_FIELD_MAP[sorted.colId]) {
      nextSort = `${SORT_FIELD_MAP[sorted.colId]},${sorted.sort}`;
    }
    if (nextSort === this.sort()) {
      return;
    }
    this.sort.set(nextSort);
    this.page.set(0);
    this.loadLogs();
  }

  onRowClicked(id: string | undefined): void {
    if (!id) {
      return;
    }
    void this.router.navigateByUrl(`${APP_PATHS.audit}/${id}`);
  }

  applyFilters(): void {
    if (!this.validateDateRange()) {
      return;
    }
    this.page.set(0);
    this.loadLogs();
  }

  clearFilters(): void {
    this.filterForm.reset({
      search: '',
      module: '',
      action: '',
      username: '',
      from: '',
      to: '',
    });
    this.page.set(0);
    this.loadLogs();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadLogs();
    }
  }

  nextPage(): void {
    const maxPage = Math.max(
      0,
      Math.ceil(this.totalElements() / this.pageSize()) - 1,
    );
    if (this.page() < maxPage) {
      this.page.update((p) => p + 1);
      this.loadLogs();
    }
  }

  loadLogs(): void {
    if (!this.validateDateRange()) {
      return;
    }

    this.loading.set(true);
    this.loadFailed.set(false);
    const filters = this.filterForm.getRawValue();

    this.auditService
      .list({
        search: filters.search.trim() || undefined,
        module: (filters.module || undefined) as AuditLog['module'] | undefined,
        action: (filters.action || undefined) as AuditLog['action'] | undefined,
        username: filters.username.trim() || undefined,
        from: this.toInstantParam(filters.from, false),
        to: this.toInstantParam(filters.to, true),
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .pipe(
        catchError(() => {
          this.loadFailed.set(true);
          return of(emptyPage<AuditLog>(this.pageSize()));
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((page) => {
        this.logs.set(page.content);
        this.totalElements.set(page.totalElements);
        this.gridApi?.setGridOption('rowData', page.content);
      });
  }

  /**
   * Ensures From ≤ To when both date filters are set. Backend remains the
   * authoritative check; this avoids a needless 400 round-trip.
   */
  private validateDateRange(): boolean {
    const { from, to } = this.filterForm.getRawValue();
    if (from && to && from > to) {
      this.notifications.error('From date must be on or before To date.');
      return false;
    }
    return true;
  }

  /**
   * Converts an HTML date input (yyyy-MM-dd) to an ISO-8601 Instant string
   * using the browser's local timezone start/end of that calendar day.
   */
  private toInstantParam(
    value: string,
    endOfDay: boolean,
  ): string | undefined {
    if (!value || !value.trim()) {
      return undefined;
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
    if (!match) {
      return undefined;
    }
    const year = Number(match[1]);
    const monthIndex = Number(match[2]) - 1;
    const day = Number(match[3]);
    const local = endOfDay
      ? new Date(year, monthIndex, day, 23, 59, 59, 999)
      : new Date(year, monthIndex, day, 0, 0, 0, 0);
    return local.toISOString();
  }
}
