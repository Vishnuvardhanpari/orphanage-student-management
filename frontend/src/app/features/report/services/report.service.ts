import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import { environment } from '../../../../environments/environment';
import { parseContentDispositionFileName } from '../../student/services/student.service';
import { ReportFileDownload, ReportFilterRequest } from '../models/report.models';

/**
 * PDF report export API client. Downloads are generated on the backend.
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/${API_PATHS.reports}`;

  exportStudent(studentId: string): Observable<ReportFileDownload> {
    return this.http
      .get(`${this.baseUrl}/student/${studentId}`, {
        observe: 'response',
        responseType: 'blob',
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map(toDownload));
  }

  exportSelected(studentIds: string[]): Observable<ReportFileDownload> {
    return this.http
      .post(
        `${this.baseUrl}/students`,
        { studentIds },
        {
          observe: 'response',
          responseType: 'blob',
          context: new HttpContext().set(SKIP_ERROR_TOAST, true),
        },
      )
      .pipe(map(toDownload));
  }

  exportFiltered(filters: ReportFilterRequest): Observable<ReportFileDownload> {
    return this.http
      .post(`${this.baseUrl}/filter`, filters, {
        observe: 'response',
        responseType: 'blob',
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map(toDownload));
  }
}

function toDownload(response: HttpResponse<Blob>): ReportFileDownload {
  return {
    blob: response.body ?? new Blob(),
    fileName: parseContentDispositionFileName(response.headers.get('Content-Disposition')),
  };
}

/** Triggers a browser download for a generated PDF blob. */
export function triggerReportDownload(file: ReportFileDownload, fallbackName: string): void {
  const url = URL.createObjectURL(file.blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = file.fileName || fallbackName;
  anchor.click();
  URL.revokeObjectURL(url);
}
