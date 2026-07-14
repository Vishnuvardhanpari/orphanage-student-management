import { Dialog } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, firstValueFrom, forkJoin } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StudentFileUpload } from '../../components/student-file-upload/student-file-upload';
import {
  CreateStudentRequest,
  DOCUMENT_TYPE_LABELS,
  DocumentType,
  Gender,
  PendingDocumentUpload,
  SUPPORTING_DOCUMENT_TYPES,
  StudentDocumentMeta,
  UpdateStudentRequest,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';
import { documentFileError } from '../../utils/student-file-validation';
import {
  dateOfBirthNotAfterAdmissionValidator,
  pastOrPresentDateValidator,
  todayIsoDate,
} from '../../validators/student-date.validators';

@Component({
  selector: 'app-student-form-page',
  standalone: true,
  imports: [
    PageHeader,
    Button,
    ReactiveFormsModule,
    StudentFileUpload,
  ],
  templateUrl: './student-form-page.html',
  styleUrl: './student-form-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentFormPage implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(Dialog);
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);

  readonly paths = APP_PATHS;
  readonly genders = Gender;
  readonly documentTypeLabels = DOCUMENT_TYPE_LABELS;
  readonly supportingDocumentTypes = SUPPORTING_DOCUMENT_TYPES;
  readonly todayMax = todayIsoDate();
  readonly mode = signal<'create' | 'edit'>('create');
  readonly studentId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly uploadProgress = signal<number | null>(null);
  readonly photo = signal<File | null>(null);
  readonly documents = signal<PendingDocumentUpload[]>([]);
  readonly existingDocuments = signal<StudentDocumentMeta[]>([]);
  readonly existingPhotoUrl = signal<string | null>(null);
  readonly photoUploading = signal(false);
  readonly docsUploading = signal(false);
  readonly deletingPhoto = signal(false);
  readonly replacingDocumentId = signal<string | null>(null);
  readonly deletingDocumentId = signal<string | null>(null);
  /** Any media operation in flight; blocks concurrent photo/document actions. */
  readonly mediaBusy = computed(
    () =>
      this.photoUploading() ||
      this.docsUploading() ||
      this.deletingPhoto() ||
      this.replacingDocumentId() !== null ||
      this.deletingDocumentId() !== null,
  );

  /** Per-document type chosen for the next replacement, keyed by document id. */
  private readonly replacementTypes = signal<Record<string, DocumentType>>({});
  private objectUrl: string | null = null;

  readonly form = this.fb.nonNullable.group(
    {
      admissionNumber: ['', [Validators.required, Validators.maxLength(50)]],
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.maxLength(100)]],
      gender: [Gender.Male, Validators.required],
      dateOfBirth: ['', [Validators.required, pastOrPresentDateValidator()]],
      bloodGroup: ['', [Validators.maxLength(10)]],
      religion: ['', [Validators.maxLength(100)]],
      nationality: ['', [Validators.maxLength(100)]],
      aadhaarNumber: [
        '',
        [Validators.maxLength(12), Validators.pattern(/^$|^\d{12}$/)],
      ],
      phoneNumber: ['', [Validators.maxLength(20)]],
      guardianName: ['', [Validators.maxLength(100)]],
      guardianRelationship: ['', [Validators.maxLength(50)]],
      guardianPhone: ['', [Validators.maxLength(20)]],
      guardianAddress: [''],
      schoolName: ['', [Validators.maxLength(255)]],
      standard: ['', [Validators.maxLength(50)]],
      medium: ['', [Validators.maxLength(50)]],
      previousSchool: ['', [Validators.maxLength(255)]],
      medicalConditions: [''],
      allergies: [''],
      disability: [''],
      emergencyNotes: [''],
      admissionDate: ['', [Validators.required, pastOrPresentDateValidator()]],
    },
    { validators: [dateOfBirthNotAfterAdmissionValidator()] },
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.mode.set('edit');
      this.studentId.set(id);
      this.form.controls.admissionNumber.disable();
      this.loadStudent(id);
    }
  }

  ngOnDestroy(): void {
    this.revokePhotoUrl();
  }

  onPhotoChange(file: File | null): void {
    this.photo.set(file);
  }

  onDocumentsChange(docs: PendingDocumentUpload[]): void {
    this.documents.set(docs);
  }

  onUploadValidationError(message: string): void {
    this.notifications.warning(message);
  }

  cancel(): void {
    const id = this.studentId();
    if (this.mode() === 'edit' && id) {
      void this.router.navigateByUrl(`${this.paths.students}/${id}`);
      return;
    }
    void this.router.navigateByUrl(this.paths.students);
  }

  hasFilesAttached(): boolean {
    return this.photo() !== null || this.documents().length > 0;
  }

  pageTitle(): string {
    return this.mode() === 'edit' ? 'Edit student' : 'Register student';
  }

  pageSubtitle(): string {
    return this.mode() === 'edit'
      ? 'Update student details, replace the photo, and manage supporting documents.'
      : 'Capture admission details, optional photo, and supporting documents.';
  }

  submitLabel(): string {
    if (this.submitting()) {
      return 'Saving…';
    }
    return this.mode() === 'edit' ? 'Save changes' : 'Register student';
  }

  documentTypeLabel(type: DocumentType): string {
    return this.documentTypeLabels[type] ?? type;
  }

  dateOfBirthError(): string | null {
    const control = this.form.controls.dateOfBirth;
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Date of birth is required.';
    }
    if (control.hasError('pastOrPresent')) {
      return 'Date of birth cannot be in the future.';
    }
    return 'Date of birth is invalid.';
  }

  admissionDateError(): string | null {
    const control = this.form.controls.admissionDate;
    if (control.touched && control.invalid) {
      if (control.hasError('required')) {
        return 'Admission date is required.';
      }
      if (control.hasError('pastOrPresent')) {
        return 'Admission date cannot be in the future.';
      }
    }
    if (
      this.form.hasError('dobAfterAdmission') &&
      (control.touched || this.form.controls.dateOfBirth.touched)
    ) {
      return 'Admission date must be on or after date of birth.';
    }
    return null;
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notifications.warning('Please fix the highlighted fields.');
      return;
    }

    if (this.mode() === 'edit') {
      if (this.hasFilesAttached()) {
        const confirmed = await this.confirm({
          title: 'Discard pending files?',
          message:
            'The selected photo or documents have not been uploaded yet. '
            + 'Saving now discards them. Use “Replace photo” or “Upload new documents” first to keep them.',
          confirmLabel: 'Save and discard',
          danger: true,
        });
        if (!confirmed) {
          return;
        }
      }
      this.submitUpdate();
      return;
    }
    this.submitCreate();
  }

  replacePhoto(): void {
    const id = this.studentId();
    const file = this.photo();
    if (!id || !file || this.mediaBusy()) {
      return;
    }
    this.photoUploading.set(true);
    this.studentService
      .replacePhoto(id, file)
      .pipe(finalize(() => this.photoUploading.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Profile photo updated.');
          this.photo.set(null);
          this.loadPhoto(id);
        },
        // Error toast comes from the global HTTP error interceptor.
        error: () => undefined,
      });
  }

  async removeExistingPhoto(): Promise<void> {
    const id = this.studentId();
    if (!id || !this.existingPhotoUrl() || this.mediaBusy()) {
      return;
    }
    const confirmed = await this.confirm({
      title: 'Remove photograph',
      message: 'Remove the current student photograph? This cannot be undone.',
      confirmLabel: 'Remove',
      danger: true,
    });
    if (!confirmed) {
      return;
    }

    this.deletingPhoto.set(true);
    this.studentService
      .deletePhoto(id)
      .pipe(finalize(() => this.deletingPhoto.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Profile photo removed.');
          this.revokePhotoUrl();
          this.existingPhotoUrl.set(null);
        },
        error: () => undefined,
      });
  }

  uploadAdditionalDocuments(): void {
    const id = this.studentId();
    const pending = this.documents();
    if (!id || pending.length === 0 || this.mediaBusy()) {
      return;
    }
    this.docsUploading.set(true);
    this.studentService
      .addDocuments(
        id,
        pending.map((d) => ({ file: d.file, documentType: d.documentType })),
      )
      .pipe(finalize(() => this.docsUploading.set(false)))
      .subscribe({
        next: (created) => {
          this.notifications.success(
            created.length === 1
              ? 'Document uploaded.'
              : `${created.length} documents uploaded.`,
          );
          this.documents.set([]);
          this.reloadDocuments(id);
        },
        error: () => undefined,
      });
  }

  replacementTypeFor(document: StudentDocumentMeta): DocumentType {
    return this.replacementTypes()[document.id] ?? document.documentType;
  }

  onReplacementTypeChange(document: StudentDocumentMeta, event: Event): void {
    const value = (event.target as HTMLSelectElement).value as DocumentType;
    this.replacementTypes.update((types) => ({ ...types, [document.id]: value }));
  }

  onReplaceDocumentSelected(document: StudentDocumentMeta, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    const id = this.studentId();
    if (!id || !file || this.mediaBusy()) {
      return;
    }

    const fileError = documentFileError(file);
    if (fileError) {
      this.notifications.warning(fileError);
      return;
    }

    this.replacingDocumentId.set(document.id);
    this.studentService
      .replaceDocument(id, document.id, file, this.replacementTypeFor(document))
      .pipe(finalize(() => this.replacingDocumentId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Document replaced.');
          this.replacementTypes.update((types) => {
            const { [document.id]: replaced, ...rest } = types;
            return rest;
          });
          this.reloadDocuments(id);
        },
        error: () => undefined,
      });
  }

  async deleteExistingDocument(document: StudentDocumentMeta): Promise<void> {
    const id = this.studentId();
    if (!id || this.mediaBusy()) {
      return;
    }
    const confirmed = await this.confirm({
      title: 'Delete document',
      message: `Delete “${document.originalFileName}”? It will no longer be listed or downloadable.`,
      confirmLabel: 'Delete',
      danger: true,
    });
    if (!confirmed) {
      return;
    }

    this.deletingDocumentId.set(document.id);
    this.studentService
      .deleteDocument(id, document.id)
      .pipe(finalize(() => this.deletingDocumentId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Document deleted.');
          this.reloadDocuments(id);
        },
        error: () => undefined,
      });
  }

  private confirm(data: ConfirmDialogData): Promise<boolean> {
    const ref = this.dialog.open<boolean, ConfirmDialogData>(ConfirmDialog, { data });
    return firstValueFrom(ref.closed, { defaultValue: false }).then(
      (result) => result === true,
    );
  }

  private submitCreate(): void {
    const raw = this.form.getRawValue();
    const payload: CreateStudentRequest = {
      admissionNumber: raw.admissionNumber.trim(),
      ...this.buildEditablePayload(raw),
    };

    this.submitting.set(true);
    this.uploadProgress.set(this.hasFilesAttached() ? 0 : null);
    this.studentService
      .create(
        payload,
        this.photo(),
        this.documents().map((d) => ({
          file: d.file,
          documentType: d.documentType,
        })),
      )
      .pipe(
        finalize(() => {
          this.submitting.set(false);
          this.uploadProgress.set(null);
        }),
      )
      .subscribe({
        next: (event) => {
          if (event.response) {
            this.notifications.success(
              `Student ${event.response.admissionNumber} registered successfully.`,
            );
            void this.router.navigateByUrl(`${APP_PATHS.students}/${event.response.id}`);
            return;
          }
          if (this.hasFilesAttached()) {
            this.uploadProgress.set(event.progress);
          }
        },
        error: () => undefined,
      });
  }

  private submitUpdate(): void {
    const id = this.studentId();
    if (!id) {
      return;
    }
    const raw = this.form.getRawValue();
    const payload: UpdateStudentRequest = this.buildEditablePayload(raw);

    this.submitting.set(true);
    this.studentService
      .update(id, payload)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Student updated successfully.');
          void this.router.navigateByUrl(`${APP_PATHS.students}/${id}`);
        },
        error: () => undefined,
      });
  }

  private buildEditablePayload(raw: ReturnType<typeof this.form.getRawValue>): UpdateStudentRequest {
    return {
      firstName: raw.firstName.trim(),
      lastName: blankToNull(raw.lastName),
      gender: raw.gender,
      dateOfBirth: raw.dateOfBirth,
      bloodGroup: blankToNull(raw.bloodGroup),
      religion: blankToNull(raw.religion),
      nationality: blankToNull(raw.nationality),
      aadhaarNumber: blankToNull(raw.aadhaarNumber),
      phoneNumber: blankToNull(raw.phoneNumber),
      guardianName: blankToNull(raw.guardianName),
      guardianRelationship: blankToNull(raw.guardianRelationship),
      guardianPhone: blankToNull(raw.guardianPhone),
      guardianAddress: blankToNull(raw.guardianAddress),
      schoolName: blankToNull(raw.schoolName),
      standard: blankToNull(raw.standard),
      medium: blankToNull(raw.medium),
      previousSchool: blankToNull(raw.previousSchool),
      medicalConditions: blankToNull(raw.medicalConditions),
      allergies: blankToNull(raw.allergies),
      disability: blankToNull(raw.disability),
      emergencyNotes: blankToNull(raw.emergencyNotes),
      admissionDate: raw.admissionDate,
    };
  }

  private loadStudent(id: string): void {
    this.loading.set(true);
    forkJoin({
      student: this.studentService.getById(id),
      documents: this.studentService.listDocuments(id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ student, documents }) => {
          this.form.patchValue({
            admissionNumber: student.admissionNumber,
            firstName: student.firstName,
            lastName: student.lastName ?? '',
            gender: student.gender,
            dateOfBirth: student.dateOfBirth,
            bloodGroup: student.bloodGroup ?? '',
            religion: student.religion ?? '',
            nationality: student.nationality ?? '',
            aadhaarNumber: student.aadhaarNumber ?? '',
            phoneNumber: student.phoneNumber ?? '',
            guardianName: student.guardianName ?? '',
            guardianRelationship: student.guardianRelationship ?? '',
            guardianPhone: student.guardianPhone ?? '',
            guardianAddress: student.guardianAddress ?? '',
            schoolName: student.schoolName ?? '',
            standard: student.standard ?? '',
            medium: student.medium ?? '',
            previousSchool: student.previousSchool ?? '',
            medicalConditions: student.medicalConditions ?? '',
            allergies: student.allergies ?? '',
            disability: student.disability ?? '',
            emergencyNotes: student.emergencyNotes ?? '',
            admissionDate: student.admissionDate,
          });
          this.existingDocuments.set(documents);
          if (student.hasProfilePhoto) {
            this.loadPhoto(id);
          } else {
            this.revokePhotoUrl();
            this.existingPhotoUrl.set(null);
          }
        },
        error: () => {
          void this.router.navigateByUrl(APP_PATHS.students);
        },
      });
  }

  private reloadDocuments(id: string): void {
    this.studentService.listDocuments(id).subscribe({
      next: (documents) => this.existingDocuments.set(documents),
      error: () => undefined,
    });
  }

  private loadPhoto(studentId: string): void {
    this.studentService.fetchPhoto(studentId).subscribe({
      next: (blob) => {
        this.revokePhotoUrl();
        this.objectUrl = URL.createObjectURL(blob);
        this.existingPhotoUrl.set(this.objectUrl);
      },
      error: () => {
        this.revokePhotoUrl();
        this.existingPhotoUrl.set(null);
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

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
