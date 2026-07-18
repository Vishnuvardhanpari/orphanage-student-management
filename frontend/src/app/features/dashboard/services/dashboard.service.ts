import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { SKIP_ERROR_TOAST } from '../../../core/interceptors/error.interceptor';
import {
  DashboardGenderCount,
  DashboardMonthlyAdmission,
  DashboardStatusCount,
  DashboardSummary,
} from '../models/dashboard.models';

/**
 * HTTP client for executive dashboard aggregates.
 *
 * All GETs opt out of the global error toast: the dashboard page loads four
 * endpoints via forkJoin and owns a single empty-state + Retry UX.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/${API_PATHS.dashboard}`;

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`, {
      context: this.skipErrorToastContext(),
    });
  }

  getAdmissions(): Observable<DashboardMonthlyAdmission[]> {
    return this.http.get<DashboardMonthlyAdmission[]>(`${this.baseUrl}/admissions`, {
      context: this.skipErrorToastContext(),
    });
  }

  getGender(): Observable<DashboardGenderCount[]> {
    return this.http.get<DashboardGenderCount[]>(`${this.baseUrl}/gender`, {
      context: this.skipErrorToastContext(),
    });
  }

  getStatus(): Observable<DashboardStatusCount[]> {
    return this.http.get<DashboardStatusCount[]>(`${this.baseUrl}/status`, {
      context: this.skipErrorToastContext(),
    });
  }

  private skipErrorToastContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
