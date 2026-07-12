import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { API_PATHS } from '../../../core/constants/api-paths';
import { APP_PATHS } from '../../../core/constants/routes';
import { UserRole } from '../../../core/enums/user-role';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import {
  AuthResponse,
  AuthUser,
  GoogleLoginRequest,
  LoginRequest,
} from '../models/auth.models';

/**
 * Authentication state and API calls for login, Google, refresh, logout, and me.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly currentUserSignal = signal<AuthUser | null>(this.tokenStorage.getUser());
  private refreshInFlight$: Observable<AuthResponse> | null = null;

  readonly currentUser = this.currentUserSignal.asReadonly();
  //readonly isAuthenticated = computed(() => !!this.tokenStorage.getAccessToken() && !!this.currentUserSignal());
  isAuthenticated(): boolean {
    return !!this.tokenStorage.getAccessToken() &&
           !!this.currentUserSignal();
  }
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === UserRole.Admin);

  constructor() {
    const user = this.tokenStorage.getUser();
    if (user && this.tokenStorage.getAccessToken()) {
      this.currentUserSignal.set(user);
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/${API_PATHS.auth.login}`, request)
      .pipe(tap((response) => this.persistSession(response)));
  }

  loginWithGoogle(request: GoogleLoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/${API_PATHS.auth.google}`, request)
      .pipe(tap((response) => this.persistSession(response)));
  }

  refresh(): Observable<AuthResponse> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }

    this.refreshInFlight$ = this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/${API_PATHS.auth.refresh}`, {
        refreshToken,
      })
      .pipe(
        tap((response) => this.persistSession(response)),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay(1),
      );

    return this.refreshInFlight$;
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${environment.apiBaseUrl}/${API_PATHS.auth.me}`).pipe(
      tap((user) => {
        this.currentUserSignal.set(user);
        const access = this.tokenStorage.getAccessToken();
        const refresh = this.tokenStorage.getRefreshToken();
        if (access && refresh) {
          this.tokenStorage.saveSession(access, refresh, user);
        }
      }),
    );
  }

  logout(navigateToLogin = true): Observable<void> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    return this.http
      .post<void>(`${environment.apiBaseUrl}/${API_PATHS.auth.logout}`, { refreshToken }, {
        observe: 'response',
      })
      .pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
        finalize(() => {
          this.clearSession();
          if (navigateToLogin) {
            void this.router.navigateByUrl(APP_PATHS.login);
          }
        }),
      );
  }

  clearSession(): void {
    this.tokenStorage.clear();
    this.currentUserSignal.set(null);
  }

  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  hasRole(roles: UserRole[]): boolean {
    const role = this.currentUserSignal()?.role;
    return !!role && roles.includes(role);
  }

  private persistSession(response: AuthResponse): void {
    this.tokenStorage.saveSession(
      response.accessToken,
      response.refreshToken,
      response.user,
    );
    this.currentUserSignal.set(response.user);

    console.log('=== Persist Session ===');
  console.log('Stored token:', this.tokenStorage.getAccessToken());
  console.log('Stored user:', this.currentUserSignal());
  console.log('Computed auth:', this.isAuthenticated());
  }
}
