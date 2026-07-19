import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type StatusBadgeTone = 'success' | 'warning' | 'error' | 'inactive' | 'info';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly tone = input<StatusBadgeTone>('info');
  readonly label = input.required<string>();
}
