import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-student-list-page',
  standalone: true,
  imports: [PageHeader, EmptyState],
  templateUrl: './student-list-page.html',
  styleUrl: './student-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentListPage {}
