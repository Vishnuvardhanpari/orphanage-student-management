import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Button } from '../button/button';
import { EmptyState } from '../empty-state/empty-state';
import { PageHeader } from '../page-header/page-header';
import { APP_PATHS } from '../../../core/constants/routes';

@Component({
  selector: 'app-error-page',
  standalone: true,
  imports: [PageHeader, EmptyState, Button],
  templateUrl: './error-page.html',
  styleUrl: './error-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorPage {
  readonly code = input(404);
  readonly title = input('Page not found');
  readonly description = input(
    'The page you requested does not exist or may have been moved.',
  );
  readonly homePath = APP_PATHS.dashboard;

  protected readonly codeLabel = computed(() => String(this.code()));
}
