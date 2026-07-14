import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
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

  readonly paths = APP_PATHS;
  readonly loading = signal(true);
  readonly student = signal<StudentDetail | null>(null);
  readonly documents = signal<StudentDocumentMeta[]>([]);
  readonly photoUrl = signal<string | null>(null);
  readonly photoUnavailable = signal(false);
  readonly downloadingId = signal<string | null>(null);

  private objectUrl: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigateByUrl(APP_PATHS.students);
      return;
    }
    this.load(id);
  }

  ngOnDestroy(): void {
    this.revokePhotoUrl();
  }

  goBack(): void {
    void this.router.navigateByUrl(this.paths.students);
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
          void this.router.navigateByUrl(APP_PATHS.students);
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
