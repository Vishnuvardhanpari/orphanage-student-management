import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StudentFileUpload } from '../../components/student-file-upload/student-file-upload';
import {
  CreateStudentRequest,
  Gender,
  PendingDocumentUpload,
} from '../../models/student.models';
import { StudentService } from '../../services/student.service';
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
export class StudentFormPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);
  private readonly notifications = inject(NotificationService);

  readonly paths = APP_PATHS;
  readonly genders = Gender;
  readonly todayMax = todayIsoDate();
  readonly submitting = signal(false);
  readonly uploadProgress = signal<number | null>(null);
  readonly photo = signal<File | null>(null);
  readonly documents = signal<PendingDocumentUpload[]>([]);

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
    void this.router.navigateByUrl(this.paths.students);
  }

  hasFilesAttached(): boolean {
    return this.photo() !== null || this.documents().length > 0;
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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notifications.warning('Please fix the highlighted fields.');
      return;
    }

    const raw = this.form.getRawValue();
    const payload: CreateStudentRequest = {
      admissionNumber: raw.admissionNumber.trim(),
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
        error: (err: HttpErrorResponse) => {
          this.notifications.error(err.error?.message || 'Registration failed.');
        },
      });
  }
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
