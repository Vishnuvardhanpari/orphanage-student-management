import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
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

const MAX_BYTES = 10 * 1024 * 1024;
const PHOTO_TYPES = new Set(['image/jpeg', 'image/png']);
const DOC_TYPES = new Set(['image/jpeg', 'image/png', 'application/pdf']);

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
    if (!PHOTO_TYPES.has(file.type) || file.size > MAX_BYTES) {
      this.validationError.emit('Photo must be JPG or PNG and at most 10 MB.');
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
      if (!DOC_TYPES.has(file.type) || file.size > MAX_BYTES) {
        this.validationError.emit(
          `${file.name} must be PDF/JPG/PNG and at most 10 MB.`,
        );
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
