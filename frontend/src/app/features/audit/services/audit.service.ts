import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import { PageResponse } from '../../../shared/models/page.models';
import { AuditListParams, AuditLog } from '../models/audit.models';

/**
 * Admin audit log API client.
 *
 * List requests opt out of the global error toast so the list page can own a
 * single empty-state + Retry UX on failure.
 */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/${API_PATHS.audit}`;

  list(params: AuditListParams = {}): Observable<PageResponse<AuditLog>> {
    let httpParams = new HttpParams();
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.module) {
      httpParams = httpParams.set('module', params.module);
    }
    if (params.action) {
      httpParams = httpParams.set('action', params.action);
    }
    if (params.username) {
      httpParams = httpParams.set('username', params.username);
    }
    if (params.entityId) {
      httpParams = httpParams.set('entityId', params.entityId);
    }
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    return this.http.get<PageResponse<AuditLog>>(this.baseUrl, {
      params: httpParams,
      context: new HttpContext().set(SKIP_ERROR_TOAST, true),
    });
  }

  getById(id: string): Observable<AuditLog> {
    return this.http.get<AuditLog>(`${this.baseUrl}/${id}`);
  }
}
