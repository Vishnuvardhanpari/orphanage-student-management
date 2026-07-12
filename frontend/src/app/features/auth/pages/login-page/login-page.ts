import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Eye, EyeOff } from 'lucide-angular';
import { Button } from '../../../../shared/components/button/button';
import { APP_PATHS } from '../../../../core/constants/routes';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { environment } from '../../../../../environments/environment';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential: string }) => void;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: Record<string, unknown>,
          ) => void;
        };
      };
    };
  }
}

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, Button, LucideAngularModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly submitting = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly googleEnabled = signal(!!environment.googleClientId);
  protected readonly icons = { Eye, EyeOff };

  private googleScript?: HTMLScriptElement;

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.router.events.subscribe(event => {
      console.log(event.constructor.name, event);
    });
    if (this.googleEnabled()) {
      this.loadGoogleScript();
    }
  }

  ngOnDestroy(): void {
    this.googleScript?.remove();
  }

  protected togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.authService.login({ username, password }).subscribe({
      next: () => {
        this.notifications.success('Signed in successfully.');
        // void this.router.navigateByUrl(APP_PATHS.dashboard);
        console.log('Current URL:', this.router.url);
        this.router.navigateByUrl('/dashboard').then(result => {
          console.log('Navigation:', result);
        });
      },
      error: () => {
        this.submitting.set(false);
      },
      complete: () => {
        this.submitting.set(false);
      },
    });
  }

  private loadGoogleScript(): void {
    if (document.getElementById('oms-google-gsi')) {
      this.renderGoogleButton();
      return;
    }

    const script = document.createElement('script');
    script.id = 'oms-google-gsi';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => this.renderGoogleButton();
    document.body.appendChild(script);
    this.googleScript = script;
  }

  private renderGoogleButton(): void {
    const container = document.getElementById('oms-google-btn');
    if (!container || !window.google?.accounts?.id || !environment.googleClientId) {
      return;
    }

    window.google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response) => this.handleGoogleCredential(response.credential),
    });

    window.google.accounts.id.renderButton(container, {
      theme: 'outline',
      size: 'large',
      width: container.offsetWidth || 320,
      text: 'continue_with',
      shape: 'rectangular',
    });
  }

  private handleGoogleCredential(idToken: string): void {
    this.submitting.set(true);
    this.authService.loginWithGoogle({ idToken }).subscribe({
      next: () => {
        this.notifications.success('Signed in with Google.');
        void this.router.navigateByUrl(APP_PATHS.dashboard);
      },
      error: () => {
        this.submitting.set(false);
      },
      complete: () => {
        this.submitting.set(false);
      },
    });
  }
}
