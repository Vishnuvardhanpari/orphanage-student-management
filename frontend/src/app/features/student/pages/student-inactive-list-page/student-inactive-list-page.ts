import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
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
import { catchError, finalize, firstValueFrom, of } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { AuthService } from '../../../auth/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { emptyPage } from '../../../../shared/models/page.models';
import {
  StudentActionsCellContext,
  StudentActionsCellRenderer,
} from '../../components/student-actions-cell-renderer/student-actions-cell-renderer';
import {
  GENDER_LABELS,
  Gender,
  StudentStatus,
  StudentSummary,
} from '../../models/student.models';
import {
  ageFromDateOfBirth,
} from '../student-list-page/student-list-page';
import { StudentService } from '../../services/student.service';

ModuleRegistry.registerModules([AllCommunityModule]);

const serverSortComparator = (): number => 0;

const SORT_FIELD_MAP: Record<string, string> = {
  admissionNumber: 'admissionNumber',
  name: 'firstName',
  gender: 'gender',
  age: 'dateOfBirth',
  schoolName: 'schoolName',
  standard: 'standard',
  admissionDate: 'admissionDate',
  status: 'status',
  deletedDate: 'deletedDate',
};

const DEFAULT_SORT = 'deletedDate,desc';

@Component({
  selector: 'app-student-inactive-list-page',
  standalone: true,
  imports: [PageHeader, EmptyState, Button, AgGridAngular],
  templateUrl: './student-inactive-list-page.html',
  styleUrl: './student-inactive-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentInactiveListPage implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dialog = inject(Dialog);

  private gridApi?: GridApi<StudentSummary>;

  readonly loading = signal(false);
  readonly loadFailed = signal(false);
  readonly students = signal<StudentSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal(DEFAULT_SORT);
  readonly paths = APP_PATHS;

  readonly gridContext: StudentActionsCellContext = {
    showEdit: false,
    showArchive: false,
    showRestore: this.authService.isAdmin(),
    onView: (student) => {
      void this.router.navigateByUrl(`${APP_PATHS.studentsInactive}/${student.id}`);
    },
    onRestore: (student) => {
      void this.restoreStudent(student);
    },
  };

  readonly columnDefs: ColDef<StudentSummary>[] = [
    {
      colId: 'admissionNumber',
      field: 'admissionNumber',
      headerName: 'Admission no.',
      width: 150,
      comparator: serverSortComparator,
    },
    {
      colId: 'name',
      headerName: 'Name',
      flex: 1.3,
      minWidth: 160,
      valueGetter: (p) =>
        [p.data?.firstName, p.data?.lastName].filter(Boolean).join(' '),
      comparator: serverSortComparator,
    },
    {
      colId: 'gender',
      field: 'gender',
      headerName: 'Gender',
      width: 110,
      valueFormatter: (p) => (p.value ? GENDER_LABELS[p.value as Gender] : ''),
      comparator: serverSortComparator,
    },
    {
      colId: 'age',
      headerName: 'Age',
      width: 90,
      valueGetter: (p) =>
        p.data?.dateOfBirth ? ageFromDateOfBirth(p.data.dateOfBirth) : null,
      comparator: serverSortComparator,
    },
    {
      colId: 'schoolName',
      field: 'schoolName',
      headerName: 'School',
      flex: 1.2,
      minWidth: 150,
      valueFormatter: (p) => p.value ?? '—',
      comparator: serverSortComparator,
    },
    {
      colId: 'admissionDate',
      field: 'admissionDate',
      headerName: 'Admitted',
      width: 130,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleDateString() : '—',
      comparator: serverSortComparator,
    },
    {
      colId: 'status',
      field: 'status',
      headerName: 'Status',
      width: 110,
      cellRenderer: (p: { value: StudentStatus | null }) => {
        if (!p.value) {
          return '';
        }
        return `<span class="student-status-badge student-status-badge--inactive">Inactive</span>`;
      },
      comparator: serverSortComparator,
    },
    {
      colId: 'deletedDate',
      field: 'deletedDate',
      headerName: 'Deleted on',
      width: 160,
      valueFormatter: (p) =>
        p.value ? new Date(p.value).toLocaleString() : '—',
      comparator: serverSortComparator,
    },
    {
      colId: 'actions',
      headerName: 'Actions',
      width: 180,
      sortable: false,
      filter: false,
      cellRenderer: StudentActionsCellRenderer,
    },
  ];

  readonly defaultColDef: ColDef = {
    sortable: true,
    resizable: true,
    unSortIcon: true,
  };

  ngOnInit(): void {
    this.loadStudents();
  }

  onGridReady(event: GridReadyEvent<StudentSummary>): void {
    this.gridApi = event.api;
  }

  onSortChanged(event: SortChangedEvent<StudentSummary>): void {
    const sorted = event.api.getColumnState().find((col) => col.sort != null);
    let nextSort = DEFAULT_SORT;
    if (sorted?.colId && sorted.sort && SORT_FIELD_MAP[sorted.colId]) {
      let direction: 'asc' | 'desc' = sorted.sort;
      if (sorted.colId === 'age') {
        direction = direction === 'asc' ? 'desc' : 'asc';
      }
      nextSort = `${SORT_FIELD_MAP[sorted.colId]},${direction}`;
    }
    if (nextSort === this.sort()) {
      return;
    }
    this.sort.set(nextSort);
    this.page.set(0);
    this.loadStudents();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadStudents();
    }
  }

  nextPage(): void {
    const maxPage = Math.max(
      0,
      Math.ceil(this.totalElements() / this.pageSize()) - 1,
    );
    if (this.page() < maxPage) {
      this.page.update((p) => p + 1);
      this.loadStudents();
    }
  }

  loadStudents(): void {
    this.loading.set(true);
    this.loadFailed.set(false);
    this.studentService
      .listInactive({
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .pipe(
        catchError(() => {
          this.loadFailed.set(true);
          return of(emptyPage<StudentSummary>(this.pageSize()));
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((page) => {
        this.students.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.gridApi?.setGridOption('rowData', page.content);
      });
  }

  async restoreStudent(student: StudentSummary): Promise<void> {
    const name = [student.firstName, student.lastName].filter(Boolean).join(' ');
    const ref = this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialog, {
      data: {
        title: 'Restore student',
        message: `Restore ${name} (${student.admissionNumber}) to the active student list?`,
        confirmLabel: 'Restore',
      },
    });
    const confirmed = await firstValueFrom(ref.closed);
    if (!confirmed) {
      return;
    }
    this.studentService.restore(student.id).subscribe({
      next: () => {
        this.notifications.success('Student restored.');
        this.loadStudents();
      },
    });
  }
}
