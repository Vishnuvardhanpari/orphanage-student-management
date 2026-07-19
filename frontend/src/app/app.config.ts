import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideToastr } from 'ngx-toastr';
import { DEFAULT_DIALOG_CONFIG } from '@angular/cdk/dialog';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

/**
 * Application providers.
 * ECharts (`provideEchartsCore`) is deferred to the dashboard feature (Milestone 11).
 * AG Grid styles are loaded globally; grid modules are imported per feature.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAnimations(),
    provideToastr({
      positionClass: 'toast-top-right',
      timeOut: 4000,
      progressBar: true,
      closeButton: true,
      newestOnTop: true,
    }),
    {
      provide: DEFAULT_DIALOG_CONFIG,
      useValue: {
        backdropClass: 'oms-dialog-backdrop',
      },
    },
  ],
};
