import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  ColDef,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  RowSelectionOptions,
  SelectionChangedEvent,
  SortChangedEvent,
} from 'ag-grid-community';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, firstValueFrom, of } from 'rxjs';
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
  ExportReportDialog,
  ExportReportDialogData,
} from '../../../report/components/export-report-dialog/export-report-dialog';
import { buildSelectionPreviewDetails } from '../../../report/models/report.models';
import {
  ReportService,
  triggerReportDownload,
} from '../../../report/services/report.service';
import { environment } from '../../../../../environments/environment';
import {
  ArchiveStudentDialog,
  ArchiveStudentDialogData,
  ArchiveStudentResult,
} from '../../components/archive-student-dialog/archive-student-dialog';
import {
  StudentActionsCellContext,
  StudentActionsCellRenderer,
} from '../../components/student-actions-cell-renderer/student-actions-cell-renderer';
import {
  GENDER_LABELS,
  Gender,
  StudentListParams,
  StudentStatus,
  StudentSummary,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';

ModuleRegistry.registerModules([AllCommunityModule]);

/** No-op comparator so AG Grid does not reorder the current page client-side. */
const serverSortComparator = (): number => 0;

/**
 * Maps grid column ids to backend sort properties. The display-only Age column
 * sorts by dateOfBirth with the direction inverted (older students first when
 * sorting age descending).
 */
const SORT_FIELD_MAP: Record<string, string> = {
  admissionNumber: 'admissionNumber',
  name: 'firstName',
  gender: 'gender',
  age: 'dateOfBirth',
  schoolName: 'schoolName',
  standard: 'standard',
  admissionDate: 'admissionDate',
  status: 'status',
};

const DEFAULT_SORT = 'admissionDate,desc';

/** Whole years between date of birth and today. */
export function ageFromDateOfBirth(dateOfBirth: string, today = new Date()): number {
  const dob = new Date(dateOfBirth);
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age -= 1;
  }
  return age;
}

@Component({
  selector: 'app-student-list-page',
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
  templateUrl: './student-list-page.html',
  styleUrl: './student-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentListPage implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly reportService = inject(ReportService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(Dialog);

  private gridApi?: GridApi<StudentSummary>;

  readonly loading = signal(false);
  /** True when the last list request failed; drives the error state + Retry. */
  readonly loadFailed = signal(false);
  readonly students = signal<StudentSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly sort = signal(DEFAULT_SORT);
  /** True when the current result set was produced with any filter applied. */
  readonly filtersActive = signal(false);
  readonly selectedCount = signal(0);
  readonly exporting = signal(false);
  readonly paths = APP_PATHS;
  readonly maxSelected = environment.reportsMaxSelected;

  /** Cross-page selection for PDF export (survives pagination and sorting). */
  private readonly selectedById = new Map<string, StudentSummary>();
  private restoringSelection = false;

  readonly rowSelection: RowSelectionOptions = {
    mode: 'multiRow',
    checkboxes: true,
    headerCheckbox: true,
    enableClickSelection: false,
  };

  /**
   * Number inputs use Angular's NumberValueAccessor, which emits
   * `number | null` (never strings), so those controls are typed accordingly.
   */
  readonly filterForm = this.fb.group({
    search: this.fb.nonNullable.control(''),
    gender: this.fb.nonNullable.control(''),
    admissionYear: this.fb.control<number | null>(null),
    school: this.fb.nonNullable.control(''),
    ageMin: this.fb.control<number | null>(null),
    ageMax: this.fb.control<number | null>(null),
  });

  readonly gridContext: StudentActionsCellContext = {
    showEdit: true,
    showArchive: true,
    onView: (student) => {
      void this.router.navigateByUrl(`${APP_PATHS.students}/${student.id}`);
    },
    onEdit: (student) => {
      void this.router.navigateByUrl(`${APP_PATHS.students}/${student.id}/edit`);
    },
    onArchive: (student) => {
      void this.archiveStudent(student);
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
      colId: 'standard',
      field: 'standard',
      headerName: 'Standard',
      width: 110,
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
        const active = p.value === StudentStatus.Active;
        const cls = active
          ? 'status-badge status-badge--success'
          : 'status-badge status-badge--inactive';
        return `<span class="${cls}">${active ? 'Active' : 'Inactive'}</span>`;
      },
      comparator: serverSortComparator,
    },
    {
      colId: 'actions',
      headerName: 'Actions',
      width: 210,
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

  onSelectionChanged(event: SelectionChangedEvent<StudentSummary>): void {
    if (this.restoringSelection) {
      return;
    }
    event.api.forEachNode((node) => {
      const row = node.data;
      if (!row) {
        return;
      }
      if (node.isSelected()) {
        this.selectedById.set(row.id, row);
      } else {
        this.selectedById.delete(row.id);
      }
    });
    this.selectedCount.set(this.selectedById.size);
  }

  onSortChanged(event: SortChangedEvent<StudentSummary>): void {
    const sorted = event.api.getColumnState().find((col) => col.sort != null);
    let nextSort = DEFAULT_SORT;
    if (sorted?.colId && sorted.sort && SORT_FIELD_MAP[sorted.colId]) {
      let direction: 'asc' | 'desc' = sorted.sort;
      if (sorted.colId === 'age') {
        // Age ascending means youngest first, i.e. latest date of birth first.
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

  applyFilters(): void {
    if (this.buildFilterParams() === null) {
      return;
    }
    this.page.set(0);
    this.loadStudents();
  }

  clearFilters(): void {
    this.filterForm.reset({
      search: '',
      gender: '',
      admissionYear: null,
      school: '',
      ageMin: null,
      ageMax: null,
    });
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
    const filters = this.buildFilterParams();
    if (filters === null) {
      return;
    }

    this.loading.set(true);
    this.loadFailed.set(false);
    this.filtersActive.set(Object.keys(filters).length > 0);

    this.studentService
      .list({
        ...filters,
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .pipe(
        // List requests set SKIP_ERROR_TOAST; this page owns EmptyState + Retry.
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
        this.restoreSelectionOnCurrentPage();
        this.selectedCount.set(this.selectedById.size);
      });
  }

  async exportSelected(): Promise<void> {
    const selected = Array.from(this.selectedById.values());
    if (selected.length === 0 || this.exporting()) {
      return;
    }
    if (selected.length > this.maxSelected) {
      this.notifications.error(
        `You can export at most ${this.maxSelected} students at a time. Deselect some students and try again.`,
      );
      return;
    }
    const ref = this.dialog.open<boolean, ExportReportDialogData>(ExportReportDialog, {
      data: {
        title: 'Export selected students',
        message: `Generate a PDF report for ${selected.length} selected student${
          selected.length === 1 ? '' : 's'
        }?`,
        details: buildSelectionPreviewDetails(selected),
        confirmLabel: 'Generate PDF',
      },
    });
    if ((await firstValueFrom(ref.closed)) !== true) {
      return;
    }

    this.exporting.set(true);
    try {
      const file = await firstValueFrom(
        this.reportService.exportSelected(selected.map((s) => s.id)),
      );
      triggerReportDownload(file, 'students-report.pdf');
      this.notifications.success('PDF report downloaded.');
      this.selectedById.clear();
      this.selectedCount.set(0);
      this.gridApi?.deselectAll();
    } catch (err) {
      this.notifications.error(await readBlobErrorMessage(err, 'PDF export failed.'));
    } finally {
      this.exporting.set(false);
    }
  }

  private restoreSelectionOnCurrentPage(): void {
    if (!this.gridApi || this.selectedById.size === 0) {
      return;
    }
    this.restoringSelection = true;
    this.gridApi.forEachNode((node) => {
      if (node.data && this.selectedById.has(node.data.id)) {
        node.setSelected(true, false);
      }
    });
    this.restoringSelection = false;
  }

  async archiveStudent(student: StudentSummary): Promise<void> {
    const name = [student.firstName, student.lastName].filter(Boolean).join(' ');
    const ref = this.dialog.open<ArchiveStudentResult | null, ArchiveStudentDialogData>(
      ArchiveStudentDialog,
      {
        data: {
          studentName: name,
          admissionNumber: student.admissionNumber,
          admissionDate: student.admissionDate,
        },
      },
    );
    const result = await firstValueFrom(ref.closed);
    if (!result) {
      return;
    }
    this.studentService.softDelete(student.id, result).subscribe({
      next: () => {
        this.notifications.success('Student archived.');
        this.loadStudents();
      },
    });
  }

  /**
   * Reads the filter form into request params, or returns null (with a toast)
   * when the admission year or age filters are invalid.
   */
  private buildFilterParams(): StudentListParams | null {
    const raw = this.filterForm.getRawValue();
    const admissionYear = toOptionalInt(raw.admissionYear);
    const ageMin = toOptionalInt(raw.ageMin);
    const ageMax = toOptionalInt(raw.ageMax);

    if (Number.isNaN(admissionYear)) {
      this.notifications.error('Admission year must be a whole number.');
      return null;
    }
    if (Number.isNaN(ageMin) || Number.isNaN(ageMax)) {
      this.notifications.error('Age filters must be whole numbers.');
      return null;
    }
    if ((ageMin !== null && ageMin < 0) || (ageMax !== null && ageMax < 0)) {
      this.notifications.error('Age filters must not be negative.');
      return null;
    }
    if (ageMin !== null && ageMax !== null && ageMin > ageMax) {
      this.notifications.error('Minimum age must not exceed maximum age.');
      return null;
    }

    const params: StudentListParams = {};
    const search = raw.search.trim();
    if (search) {
      params.search = search;
    }
    if (raw.gender) {
      params.gender = raw.gender as Gender;
    }
    if (admissionYear !== null) {
      params.admissionYear = admissionYear;
    }
    const school = raw.school.trim();
    if (school) {
      params.school = school;
    }
    if (ageMin !== null) {
      params.ageMin = ageMin;
    }
    if (ageMax !== null) {
      params.ageMax = ageMax;
    }
    return params;
  }
}

/**
 * Normalizes an optional whole-number filter value from a number input.
 * Accepts numbers (legacy NumberValueAccessor) and digit strings (`app-input` CVA).
 * Returns null for blank input and NaN for non-integer input so callers can
 * surface a validation error instead of silently dropping the filter.
 */
function toOptionalInt(value: number | string | null): number | null {
  if (value === null || value === '') {
    return null;
  }
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n)) {
    return Number.NaN;
  }
  return Number.isInteger(n) ? n : Number.NaN;
}

async function readBlobErrorMessage(err: unknown, fallback: string): Promise<string> {
  if (!(err instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (typeof err.error?.message === 'string') {
    return err.error.message;
  }
  if (err.error instanceof Blob) {
    try {
      const text = await err.error.text();
      const json = JSON.parse(text) as { message?: string };
      if (json.message) {
        return json.message;
      }
    } catch {
      // ignore
    }
  }
  return fallback;
}
