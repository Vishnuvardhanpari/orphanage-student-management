import { Injectable, signal, effect } from '@angular/core';
import { STORAGE_KEYS } from '../constants/storage-keys';

export type ThemeMode = 'light' | 'dark';

/**
 * Manages light/dark theme via the `dark` class on documentElement.
 * Toggle UI deferred to Milestone 13; structure is dark-mode ready.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly mode = signal<ThemeMode>(this.readInitialMode());

  constructor() {
    effect(() => {
      const mode = this.mode();
      document.documentElement.classList.toggle('dark', mode === 'dark');
      try {
        localStorage.setItem(STORAGE_KEYS.themeMode, mode);
      } catch {
        // Storage may be unavailable (private browsing); ignore.
      }
    });
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
  }

  toggle(): void {
    this.mode.update((current) => (current === 'light' ? 'dark' : 'light'));
  }

  private readInitialMode(): ThemeMode {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.themeMode);
      if (stored === 'light' || stored === 'dark') {
        return stored;
      }
    } catch {
      // ignore
    }
    return 'light';
  }
}
