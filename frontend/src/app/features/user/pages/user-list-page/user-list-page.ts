import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [PageHeader, EmptyState],
  templateUrl: './user-list-page.html',
  styleUrl: './user-list-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserListPage {}
