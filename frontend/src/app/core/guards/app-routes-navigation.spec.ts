import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet } from '@angular/router';
import { Component, importProvidersFrom } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';
import { APP_ROUTES } from '../constants/routes';
import { signal } from '@angular/core';
import { MainLayout } from '../../layout/pages/main-layout/main-layout';

/**
 * Mirrors app.routes.ts shape: auth sibling + MainLayout parent with authGuard.
 */
@Component({ standalone: true, template: 'login-page' })
class LoginStub {}

@Component({ standalone: true, template: 'dashboard-page' })
class DashboardStub {}

describe('app-shaped routes after login', () => {
  let router: Router;
  let authenticated: boolean;

  beforeEach(async () => {
    authenticated = false;

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: APP_ROUTES.auth,
            children: [
              {
                path: APP_ROUTES.login,
                component: LoginStub,
                canActivate: [guestGuard],
              },
            ],
          },
          {
            path: '',
            component: DashboardStub, // stand-in for MainLayout shell
            canActivate: [authGuard],
            children: [
              {
                path: '',
                pathMatch: 'full',
                redirectTo: APP_ROUTES.dashboard,
              },
              {
                path: APP_ROUTES.dashboard,
                component: DashboardStub,
              },
            ],
          },
          {
            path: '**',
            redirectTo: APP_ROUTES.dashboard,
          },
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

  it('reproduces login success then dashboard navigation', async () => {
    authenticated = false;
    await router.navigateByUrl('/auth/login');
    expect(router.url).toBe('/auth/login');

    // Simulate AuthService.persistSession completing before navigateByUrl
    authenticated = true;
    const navigated = await router.navigateByUrl('/dashboard');

    expect(navigated).toBeTrue();
    expect(router.url).toBe('/dashboard');
  });

  it('bounces unauthenticated dashboard access back to login', async () => {
    authenticated = false;
    await router.navigateByUrl('/dashboard');
    expect(router.url).toBe('/auth/login');
  });
});
