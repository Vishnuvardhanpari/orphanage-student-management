import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, FileText, Image, Trash2, Upload } from 'lucide-angular';
import { Button } from '../../../../shared/components/button/button';
import {
  DocumentType,
  PendingDocumentUpload,
  SUPPORTING_DOCUMENT_TYPES,
} from '../../models/student.models';
import {
  documentFileError,
  photoFileError,
} from '../../utils/student-file-validation';

@Component({
  selector: 'app-student-file-upload',
  standalone: true,
  imports: [FormsModule, Button, LucideAngularModule],
  templateUrl: './student-file-upload.html',
  styleUrl: './student-file-upload.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentFileUpload implements OnDestroy {
  readonly photo = input<File | null>(null);
  readonly documents = input<PendingDocumentUpload[]>([]);

  readonly photoChange = output<File | null>();
  readonly documentsChange = output<PendingDocumentUpload[]>();
  readonly validationError = output<string>();

  readonly photoPreviewUrl = signal<string | null>(null);
  readonly documentTypes = SUPPORTING_DOCUMENT_TYPES;
  readonly icons = { Upload, Trash2, FileText, Image };

  private objectUrls: string[] = [];

  constructor() {
    // Keep the local preview in sync with the parent-owned pending photo so
    // that clearing the selection upstream (e.g. after a successful replace)
    // also removes the stale preview.
    effect(() => {
      if (this.photo() === null && this.photoPreviewUrl() !== null) {
        this.revokeUrl(this.photoPreviewUrl());
        this.photoPreviewUrl.set(null);
      }
    });
  }

  ngOnDestroy(): void {
    this.revokeAll();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const photoError = photoFileError(file);
    if (photoError) {
      this.validationError.emit(photoError);
      return;
    }
    this.revokeUrl(this.photoPreviewUrl());
    const url = URL.createObjectURL(file);
    this.objectUrls.push(url);
    this.photoPreviewUrl.set(url);
    this.photoChange.emit(file);
  }

  clearPhoto(): void {
    this.revokeUrl(this.photoPreviewUrl());
    this.photoPreviewUrl.set(null);
    this.photoChange.emit(null);
  }

  onDocumentsSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (files.length === 0) {
      return;
    }

    const next = [...this.documents()];
    for (const file of files) {
      const docError = documentFileError(file);
      if (docError) {
        this.validationError.emit(docError);
        continue;
      }
      let previewUrl: string | undefined;
      if (file.type.startsWith('image/')) {
        previewUrl = URL.createObjectURL(file);
        this.objectUrls.push(previewUrl);
      }
      next.push({
        file,
        documentType: DocumentType.Other,
        previewUrl,
      });
    }
    this.documentsChange.emit(next);
  }

  updateDocumentType(index: number, documentType: DocumentType): void {
    const next = this.documents().map((doc, i) =>
      i === index ? { ...doc, documentType } : doc,
    );
    this.documentsChange.emit(next);
  }

  removeDocument(index: number): void {
    const target = this.documents()[index];
    if (target?.previewUrl) {
      this.revokeUrl(target.previewUrl);
    }
    const next = this.documents().filter((_, i) => i !== index);
    this.documentsChange.emit(next);
  }

  documentTypeLabel(type: DocumentType): string {
    return type
      .split('_')
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(' ');
  }

  private revokeUrl(url: string | null | undefined): void {
    if (!url) {
      return;
    }
    URL.revokeObjectURL(url);
    this.objectUrls = this.objectUrls.filter((u) => u !== url);
  }

  private revokeAll(): void {
    for (const url of this.objectUrls) {
      URL.revokeObjectURL(url);
    }
    this.objectUrls = [];
  }
}
