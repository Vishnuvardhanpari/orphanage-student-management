import { Injectable } from '@angular/core';
import { STORAGE_KEYS } from '../constants/storage-keys';
import { AuthUser } from '../../features/auth/models/auth.models';

/**
 * sessionStorage-backed token and user persistence.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getAccessToken(): string | null {
    return sessionStorage.getItem(STORAGE_KEYS.accessToken);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(STORAGE_KEYS.refreshToken);
  }

  getUser(): AuthUser | null {
    const raw = sessionStorage.getItem(STORAGE_KEYS.currentUser);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  saveSession(accessToken: string, refreshToken: string, user: AuthUser): void {
    sessionStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
    sessionStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
    sessionStorage.setItem(STORAGE_KEYS.currentUser, JSON.stringify(user));
  }

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEYS.accessToken);
    sessionStorage.removeItem(STORAGE_KEYS.refreshToken);
    sessionStorage.removeItem(STORAGE_KEYS.currentUser);
  }
}
