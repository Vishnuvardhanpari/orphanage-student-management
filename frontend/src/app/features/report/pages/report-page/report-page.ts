import { HttpErrorResponse } from '@angular/common/http';
import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StudentService } from '../../../student/services/student.service';
import { Gender } from '../../../student/models/student.models';
import {
  ExportReportDialog,
  ExportReportDialogData,
} from '../../components/export-report-dialog/export-report-dialog';
import {
  ReportFilterRequest,
  ReportFileDownload,
  ReportStudentScope,
  summarizeReportFilters,
} from '../../models/report.models';
import { ReportService, triggerReportDownload } from '../../services/report.service';

@Component({
  selector: 'app-report-page',
  standalone: true,
  imports: [PageHeader, Button, ReactiveFormsModule, RouterLink],
  templateUrl: './report-page.html',
  styleUrl: './report-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportPage {
  private readonly reportService = inject(ReportService);
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);
  private readonly fb = inject(FormBuilder);

  readonly paths = APP_PATHS;
  readonly exporting = signal(false);
  readonly singleLookupBusy = signal(false);
  readonly filterPreviewBusy = signal(false);
  readonly maxFilterResults = environment.reportsMaxFilterResults;

  readonly singleForm = this.fb.group({
    admissionNumber: this.fb.nonNullable.control(''),
  });

  readonly filterForm = this.fb.group({
    scope: this.fb.nonNullable.control<ReportStudentScope>('ACTIVE'),
    search: this.fb.nonNullable.control(''),
    gender: this.fb.nonNullable.control(''),
    admissionYear: this.fb.control<number | null>(null),
    school: this.fb.nonNullable.control(''),
    ageMin: this.fb.control<number | null>(null),
    ageMax: this.fb.control<number | null>(null),
  });

  async exportByAdmissionNumber(): Promise<void> {
    const admissionNumber = this.singleForm.controls.admissionNumber.value.trim();
    if (!admissionNumber) {
      this.notifications.warning('Enter an admission number.');
      return;
    }
    if (this.exporting() || this.singleLookupBusy()) {
      return;
    }

    this.singleLookupBusy.set(true);
    try {
      const page = await firstValueFrom(
        this.studentService.list({
          admissionNumber,
          page: 0,
          size: 1,
        }),
      );
      const match = page.content[0];
      if (!match) {
        this.notifications.error('No active student found with that admission number.');
        return;
      }

      const confirmed = await this.confirmExport({
        title: 'Export student PDF',
        message: `Generate a PDF report for ${match.firstName}${
          match.lastName ? ' ' + match.lastName : ''
        } (${match.admissionNumber})?`,
        confirmLabel: 'Generate PDF',
      });
      if (!confirmed) {
        return;
      }

      await this.runExport(
        this.reportService.exportStudent(match.id),
        `student-report-${match.admissionNumber}.pdf`,
      );
    } catch (err) {
      this.notifications.error(await readHttpErrorMessage(err, 'Could not look up student.'));
    } finally {
      this.singleLookupBusy.set(false);
    }
  }

  async exportFiltered(): Promise<void> {
    if (this.exporting() || this.filterPreviewBusy()) {
      return;
    }
    const filters = this.buildFilters();
    if (filters === null) {
      return;
    }

    this.filterPreviewBusy.set(true);
    let matchCount: number;
    try {
      matchCount = await this.countMatchingStudents(filters);
    } catch (err) {
      this.notifications.error(await readHttpErrorMessage(err, 'Could not preview filter matches.'));
      return;
    } finally {
      this.filterPreviewBusy.set(false);
    }

    if (matchCount === 0) {
      this.notifications.warning('No students match the selected filters.');
      return;
    }
    if (matchCount > this.maxFilterResults) {
      this.notifications.error(
        `Filter matched ${matchCount} students; maximum allowed is ${this.maxFilterResults}. Narrow your filters and try again.`,
      );
      return;
    }

    const confirmed = await this.confirmExport({
      title: 'Export filtered students',
      message: `Generate a PDF for ${matchCount} matching student${
        matchCount === 1 ? '' : 's'
      }?`,
      details: summarizeReportFilters(filters),
      confirmLabel: 'Generate PDF',
    });
    if (!confirmed) {
      return;
    }

    await this.runExport(this.reportService.exportFiltered(filters), 'students-report.pdf');
  }

  private buildFilters(): ReportFilterRequest | null {
    const raw = this.filterForm.getRawValue();
    if (raw.ageMin != null && raw.ageMax != null && raw.ageMin > raw.ageMax) {
      this.notifications.warning('Minimum age cannot be greater than maximum age.');
      return null;
    }
    return {
      scope: raw.scope,
      search: raw.search.trim() || null,
      gender: raw.gender || null,
      admissionYear: raw.admissionYear,
      school: raw.school.trim() || null,
      ageMin: raw.ageMin,
      ageMax: raw.ageMax,
    };
  }

  private async countMatchingStudents(filters: ReportFilterRequest): Promise<number> {
    const scope = filters.scope ?? 'ACTIVE';
    const gender = parseGender(filters.gender);
    const listParams = {
      search: filters.search?.trim() || undefined,
      gender,
      admissionYear: filters.admissionYear,
      school: filters.school?.trim() || undefined,
      ageMin: filters.ageMin,
      ageMax: filters.ageMax,
      page: 0,
      size: 1,
    };

    if (scope === 'ACTIVE') {
      const page = await firstValueFrom(this.studentService.list(listParams));
      return page.totalElements;
    }
    if (scope === 'ARCHIVED') {
      const page = await firstValueFrom(this.studentService.listInactive(listParams));
      return page.totalElements;
    }

    const [active, archived] = await Promise.all([
      firstValueFrom(this.studentService.list(listParams)),
      firstValueFrom(this.studentService.listInactive(listParams)),
    ]);
    return active.totalElements + archived.totalElements;
  }

  private async confirmExport(data: ExportReportDialogData): Promise<boolean> {
    const ref = this.dialog.open<boolean, ExportReportDialogData>(ExportReportDialog, {
      data,
    });
    return (await firstValueFrom(ref.closed)) === true;
  }

  private async runExport(
    request: Observable<ReportFileDownload>,
    fallbackName: string,
  ): Promise<void> {
    this.exporting.set(true);
    try {
      const file = await firstValueFrom(request);
      triggerReportDownload(file, fallbackName);
      this.notifications.success('PDF report downloaded.');
    } catch (err) {
      this.notifications.error(await readHttpErrorMessage(err, 'PDF export failed.'));
    } finally {
      this.exporting.set(false);
    }
  }
}

async function readHttpErrorMessage(err: unknown, fallback: string): Promise<string> {
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
      // ignore parse errors
    }
  }
  return fallback;
}

function parseGender(value: string | null | undefined): Gender | undefined {
  if (value === Gender.Male || value === Gender.Female || value === Gender.Other) {
    return value;
  }
  return undefined;
}
