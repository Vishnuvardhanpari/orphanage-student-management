import { DatePipe, DecimalPipe } from '@angular/common';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, firstValueFrom, forkJoin } from 'rxjs';
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
import {
  ArchiveStudentDialog,
  ArchiveStudentDialogData,
  ArchiveStudentResult,
} from '../../components/archive-student-dialog/archive-student-dialog';
import {
  DOCUMENT_TYPE_LABELS,
  DocumentType,
  GENDER_LABELS,
  Gender,
  StudentDetail,
  StudentDocumentMeta,
  StudentStatus,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';

@Component({
  selector: 'app-student-profile-page',
  standalone: true,
  imports: [PageHeader, Button, DatePipe, DecimalPipe, EmptyState],
  templateUrl: './student-profile-page.html',
  styleUrl: './student-profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentProfilePage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(Dialog);

  readonly paths = APP_PATHS;
  /**
   * Hint from static route config, used only before the student loads (e.g.
   * to pick the right "back" target while the request is in flight).
   */
  private readonly routeArchivedHint = signal(false);
  readonly isAdmin = this.authService.isAdmin;
  readonly loading = signal(true);
  readonly student = signal<StudentDetail | null>(null);
  readonly documents = signal<StudentDocumentMeta[]>([]);
  readonly photoUrl = signal<string | null>(null);
  readonly photoUnavailable = signal(false);
  readonly downloadingId = signal<string | null>(null);
  readonly actionBusy = signal(false);
  /**
   * Archived state derived from the loaded student's actual status (Milestone
   * 9 QA — BUG-001), not from which route was used to reach this page. Today
   * `INACTIVE` is only ever produced by soft-delete, so it is a reliable,
   * backend-authoritative signal for "this record is archived". Falls back to
   * the route hint only while the student has not loaded yet.
   */
  readonly archived = computed(() => {
    const student = this.student();
    return student ? student.status === StudentStatus.Inactive : this.routeArchivedHint();
  });

  private objectUrl: string | null = null;

  ngOnInit(): void {
    this.routeArchivedHint.set(!!this.route.snapshot.data['archived']);
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigateByUrl(this.listPath());
      return;
    }
    this.load(id);
  }

  ngOnDestroy(): void {
    this.revokePhotoUrl();
  }

  goBack(): void {
    void this.router.navigateByUrl(this.listPath());
  }

  editStudent(): void {
    const student = this.student();
    if (!student || this.archived()) {
      return;
    }
    void this.router.navigateByUrl(`${this.paths.students}/${student.id}/edit`);
  }

  async archiveStudent(): Promise<void> {
    const student = this.student();
    if (!student || this.archived() || this.actionBusy()) {
      return;
    }
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
    this.actionBusy.set(true);
    this.studentService
      .softDelete(student.id, result)
      .pipe(finalize(() => this.actionBusy.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Student archived.');
          void this.router.navigateByUrl(this.paths.students);
        },
      });
  }

  async restoreStudent(): Promise<void> {
    const student = this.student();
    if (!student || !this.archived() || !this.isAdmin() || this.actionBusy()) {
      return;
    }
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
    this.actionBusy.set(true);
    this.studentService
      .restore(student.id)
      .pipe(finalize(() => this.actionBusy.set(false)))
      .subscribe({
        next: (restored) => {
          this.notifications.success('Student restored.');
          void this.router.navigateByUrl(`${this.paths.students}/${restored.id}`);
        },
      });
  }

  statusLabel(status: StudentStatus): string {
    return status === StudentStatus.Active ? 'Active' : 'Inactive';
  }

  genderLabel(gender: Gender): string {
    return GENDER_LABELS[gender] ?? gender;
  }

  documentTypeLabel(type: DocumentType): string {
    return DOCUMENT_TYPE_LABELS[type] ?? type;
  }

  display(value: string | null | undefined): string {
    if (value == null || value.trim() === '') {
      return '—';
    }
    return value;
  }

  download(document: StudentDocumentMeta): void {
    const student = this.student();
    if (!student || this.downloadingId()) {
      return;
    }
    this.downloadingId.set(document.id);
    this.studentService
      .downloadDocument(student.id, document.id)
      .pipe(finalize(() => this.downloadingId.set(null)))
      .subscribe({
        next: ({ blob, fileName }) => {
          const url = URL.createObjectURL(blob);
          const anchor = globalThis.document.createElement('a');
          anchor.href = url;
          anchor.download = fileName || document.originalFileName;
          anchor.click();
          URL.revokeObjectURL(url);
        },
        error: (err: HttpErrorResponse) => {
          this.notifications.error(err.error?.message || 'Download failed.');
        },
      });
  }

  private listPath(): string {
    return this.archived() ? this.paths.studentsInactive : this.paths.students;
  }

  private load(id: string): void {
    this.loading.set(true);
    forkJoin({
      student: this.studentService.getById(id),
      documents: this.studentService.listDocuments(id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ student, documents }) => {
          this.student.set(student);
          this.documents.set(documents);
          if (student.hasProfilePhoto) {
            this.loadPhoto(student.id);
          } else {
            this.revokePhotoUrl();
            this.photoUrl.set(null);
            this.photoUnavailable.set(false);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.notifications.error(err.error?.message || 'Student not found.');
          void this.router.navigateByUrl(this.listPath());
        },
      });
  }

  private loadPhoto(studentId: string): void {
    this.photoUnavailable.set(false);
    this.studentService.fetchPhoto(studentId).subscribe({
      next: (blob) => {
        this.revokePhotoUrl();
        this.objectUrl = URL.createObjectURL(blob);
        this.photoUrl.set(this.objectUrl);
        this.photoUnavailable.set(false);
      },
      error: () => {
        this.revokePhotoUrl();
        this.photoUrl.set(null);
        this.photoUnavailable.set(true);
        this.notifications.error('Could not load the student photograph.');
      },
    });
  }

  private revokePhotoUrl(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
