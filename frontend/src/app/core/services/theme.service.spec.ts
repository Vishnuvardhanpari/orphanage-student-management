import { TestBed } from '@angular/core/testing';
import { STORAGE_KEYS } from '../constants/storage-keys';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.removeItem(STORAGE_KEYS.themeMode);
    document.documentElement.classList.remove('dark');
    TestBed.configureTestingModule({
      providers: [ThemeService],
    });
  });

  afterEach(() => {
    localStorage.removeItem(STORAGE_KEYS.themeMode);
    document.documentElement.classList.remove('dark');
  });

  it('defaults to light mode', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();
    expect(service.mode()).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBeFalse();
  });

  it('toggles and persists dark mode', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();

    service.toggle();
    TestBed.flushEffects();
    expect(service.mode()).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBeTrue();
    expect(localStorage.getItem(STORAGE_KEYS.themeMode)).toBe('dark');

    service.toggle();
    TestBed.flushEffects();
    expect(service.mode()).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEYS.themeMode)).toBe('light');
  });

  it('restores stored preference on construct', () => {
    localStorage.setItem(STORAGE_KEYS.themeMode, 'dark');
    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();
    expect(service.mode()).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBeTrue();
  });
});
