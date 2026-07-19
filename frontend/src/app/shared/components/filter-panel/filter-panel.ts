import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Card } from '../card/card';

/** Glass filter bar shell for list pages. */
@Component({
  selector: 'app-filter-panel',
  standalone: true,
  imports: [Card],
  templateUrl: './filter-panel.html',
  styleUrl: './filter-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilterPanel {
  readonly ariaLabel = input('Filters');
  readonly columns = input<'2' | '3' | '4'>('4');
}
