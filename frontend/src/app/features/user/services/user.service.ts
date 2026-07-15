import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { PageResponse } from '../../../shared/models/page.models';
import {
  CreateUserRequest,
  ManagedUser,
  ResetPasswordRequest,
  UpdateUserRequest,
  UserListParams,
} from '../models/user.models';

/**
 * Admin user management API client.
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/${API_PATHS.users}`;

  list(params: UserListParams = {}): Observable<PageResponse<ManagedUser>> {
    let httpParams = new HttpParams();
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.role) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.enabled !== undefined && params.enabled !== null) {
      httpParams = httpParams.set('enabled', String(params.enabled));
    }
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 20));
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    return this.http.get<PageResponse<ManagedUser>>(this.baseUrl, { params: httpParams });
  }

  getById(id: string): Observable<ManagedUser> {
    return this.http.get<ManagedUser>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateUserRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(this.baseUrl, request);
  }

  update(id: string, request: UpdateUserRequest): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.baseUrl}/${id}`, request);
  }

  disable(id: string): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.baseUrl}/${id}/disable`, {});
  }

  enable(id: string): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.baseUrl}/${id}/enable`, {});
  }

  resetPassword(id: string, request: ResetPasswordRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.baseUrl}/${id}/reset-password`, request);
  }
}
