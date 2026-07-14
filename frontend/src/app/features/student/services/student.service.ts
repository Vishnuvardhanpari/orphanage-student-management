import {
  HttpClient,
  HttpContext,
  HttpEvent,
  HttpEventType,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, filter, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import {
  CreateStudentRequest,
  DocumentType,
  StudentCreatedResponse,
  StudentDetail,
  StudentDocumentMeta,
  UpdateStudentRequest,
} from '../models/student.models';

export interface StudentCreateProgress {
  /** Upload percent 0–100, or null when total size is unknown / not an upload event. */
  progress: number | null;
  response?: StudentCreatedResponse;
}

export interface StudentFileDownload {
  blob: Blob;
  /** Parsed from Content-Disposition when the header is readable; otherwise null. */
  fileName: string | null;
}

/**
 * Student registration, profile, and update API client.
 */
@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/${API_PATHS.students}`;

  /**
   * Creates a student and reports multipart upload progress.
   */
  create(
    data: CreateStudentRequest,
    photo: File | null,
    documents: { file: File; documentType: DocumentType }[],
  ): Observable<StudentCreateProgress> {
    const formData = new FormData();
    formData.append(
      'data',
      new Blob([JSON.stringify(data)], { type: 'application/json' }),
    );

    if (photo) {
      formData.append('photo', photo, photo.name);
    }

    for (const doc of documents) {
      formData.append('documents', doc.file, doc.file.name);
      formData.append('documentTypes', doc.documentType);
    }

    return this.http
      .post<StudentCreatedResponse>(this.baseUrl, formData, {
        reportProgress: true,
        observe: 'events',
      })
      .pipe(
        filter(
          (event: HttpEvent<StudentCreatedResponse>) =>
            event.type === HttpEventType.UploadProgress ||
            event.type === HttpEventType.Response,
        ),
        map((event: HttpEvent<StudentCreatedResponse>): StudentCreateProgress => {
          if (event.type === HttpEventType.UploadProgress) {
            const total = event.total;
            if (total && total > 0) {
              return {
                progress: Math.min(100, Math.round((100 * event.loaded) / total)),
              };
            }
            return { progress: null };
          }
          if (event.type === HttpEventType.Response) {
            return {
              progress: 100,
              response: event.body ?? undefined,
            };
          }
          return { progress: null };
        }),
      );
  }

  update(id: string, data: UpdateStudentRequest): Observable<StudentDetail> {
    return this.http.put<StudentDetail>(`${this.baseUrl}/${id}`, data);
  }

  replacePhoto(id: string, photo: File): Observable<void> {
    const formData = new FormData();
    formData.append('photo', photo, photo.name);
    return this.http.put<void>(`${this.baseUrl}/${id}/photo`, formData);
  }

  addDocuments(
    studentId: string,
    documents: { file: File; documentType: DocumentType }[],
  ): Observable<StudentDocumentMeta[]> {
    const formData = new FormData();
    for (const doc of documents) {
      formData.append('documents', doc.file, doc.file.name);
      formData.append('documentTypes', doc.documentType);
    }
    return this.http.post<StudentDocumentMeta[]>(
      `${this.baseUrl}/${studentId}/documents`,
      formData,
    );
  }

  replaceDocument(
    studentId: string,
    documentId: string,
    document: File,
    documentType?: DocumentType,
  ): Observable<StudentDocumentMeta> {
    const formData = new FormData();
    formData.append('document', document, document.name);
    if (documentType) {
      formData.append('documentType', documentType);
    }
    return this.http.put<StudentDocumentMeta>(
      `${this.baseUrl}/${studentId}/documents/${documentId}`,
      formData,
    );
  }

  deletePhoto(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/photo`);
  }

  deleteDocument(studentId: string, documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${studentId}/documents/${documentId}`);
  }

  getById(id: string): Observable<StudentDetail> {
    return this.http.get<StudentDetail>(`${this.baseUrl}/${id}`);
  }

  listDocuments(studentId: string): Observable<StudentDocumentMeta[]> {
    return this.http.get<StudentDocumentMeta[]>(`${this.baseUrl}/${studentId}/documents`);
  }

  fetchPhoto(studentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${studentId}/photo`, {
      responseType: 'blob',
      // Blob error bodies cannot be resolved to a message by the global
      // interceptor; callers show their own contextual toast instead.
      context: new HttpContext().set(SKIP_ERROR_TOAST, true),
    });
  }

  downloadDocument(studentId: string, documentId: string): Observable<StudentFileDownload> {
    return this.http
      .get(`${this.baseUrl}/${studentId}/documents/${documentId}/download`, {
        observe: 'response',
        responseType: 'blob',
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(
        map((response: HttpResponse<Blob>) => ({
          blob: response.body ?? new Blob(),
          fileName: parseContentDispositionFileName(
            response.headers.get('Content-Disposition'),
          ),
        })),
      );
  }
}

/** Parses RFC 5987 / quoted filename from Content-Disposition. */
export function parseContentDispositionFileName(header: string | null): string | null {
  if (!header) {
    return null;
  }
  const utf8Match = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(header);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim());
    } catch {
      return utf8Match[1].trim();
    }
  }
  const quotedMatch = /filename\s*=\s*"([^"]+)"/i.exec(header);
  if (quotedMatch?.[1]) {
    return quotedMatch[1];
  }
  const plainMatch = /filename\s*=\s*([^;]+)/i.exec(header);
  return plainMatch?.[1]?.trim() ?? null;
}
