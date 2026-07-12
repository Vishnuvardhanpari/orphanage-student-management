import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-report-page',
  standalone: true,
  imports: [PageHeader, EmptyState],
  templateUrl: './report-page.html',
  styleUrl: './report-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportPage {}
