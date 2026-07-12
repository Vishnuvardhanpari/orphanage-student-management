import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../../features/auth/services/auth.service';

describe('authGuard', () => {
  it('allows authenticated users', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => true } },
      ],
    });

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never),
    );
    expect(result).toBeTrue();
  });

  it('redirects unauthenticated users to login', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => false } },
      ],
    });

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never),
    );
    expect(String(result)).toContain('auth/login');
  });
});
