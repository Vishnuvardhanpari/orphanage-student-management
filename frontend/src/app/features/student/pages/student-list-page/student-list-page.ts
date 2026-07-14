import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { APP_PATHS } from '../../../../core/constants/routes';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';

@Component({
  selector: 'app-student-list-page',
  standalone: true,
  imports: [PageHeader, EmptyState, Button],
  templateUrl: './student-list-page.html',
  styleUrl: './student-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentListPage {
  private readonly router = inject(Router);

  readonly paths = APP_PATHS;

  goToRegister(): void {
    void this.router.navigate([this.paths.students, 'new']);
  }
}
