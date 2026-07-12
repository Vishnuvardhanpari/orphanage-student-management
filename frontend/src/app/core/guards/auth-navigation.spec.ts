import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Component } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';
import { UserRole } from '../enums/user-role';
import { signal } from '@angular/core';

@Component({ standalone: true, template: 'login' })
class LoginStub {}

@Component({ standalone: true, template: 'dashboard' })
class DashboardStub {}

describe('login to dashboard navigation with guards', () => {
  let router: Router;
  let authenticated: boolean;

  beforeEach(async () => {
    authenticated = false;

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'auth/login',
            component: LoginStub,
            canActivate: [guestGuard],
          },
          {
            path: 'dashboard',
            component: DashboardStub,
            canActivate: [authGuard],
          },
          { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
        ]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
            currentUser: signal(null).asReadonly(),
            isAdmin: () => false,
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('allows navigation to dashboard when authenticated after login', async () => {
    await router.navigateByUrl('/auth/login');
    expect(router.url).toBe('/auth/login');

    authenticated = true;
    const ok = await router.navigateByUrl('/dashboard');

    expect(ok).toBeTrue();
    expect(router.url).toBe('/dashboard');
  });

  it('blocks dashboard and stays away when not authenticated', async () => {
    authenticated = false;
    const ok = await router.navigateByUrl('/dashboard');

    expect(ok).toBeTrue(); // redirect counts as successful navigation
    expect(router.url).toBe('/auth/login');
  });

  it('guestGuard sends authenticated user away from login to dashboard', async () => {
    authenticated = true;
    await router.navigateByUrl('/auth/login');
    expect(router.url).toBe('/dashboard');
  });
});
