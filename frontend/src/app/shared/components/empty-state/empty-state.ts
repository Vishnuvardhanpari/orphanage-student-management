import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type EmptyStateVariant = 'default' | 'error';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly description = input('');
  readonly variant = input<EmptyStateVariant>('default');
}
