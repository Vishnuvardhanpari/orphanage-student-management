import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, filter, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import {
  CreateStudentRequest,
  DocumentType,
  StudentCreatedResponse,
} from '../models/student.models';

export interface StudentCreateProgress {
  /** Upload percent 0–100, or null when total size is unknown / not an upload event. */
  progress: number | null;
  response?: StudentCreatedResponse;
}

/**
 * Student registration API client.
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
}
