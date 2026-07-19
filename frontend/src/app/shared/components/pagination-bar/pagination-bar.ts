import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Button } from '../button/button';

@Component({
  selector: 'app-pagination-bar',
  standalone: true,
  imports: [Button],
  templateUrl: './pagination-bar.html',
  styleUrl: './pagination-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationBar {
  readonly page = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly loading = input(false);
  readonly itemLabel = input('items');

  readonly previous = output<void>();
  readonly next = output<void>();

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalElements() / Math.max(1, this.pageSize()))),
  );

  protected readonly meta = computed(() => {
    const total = this.totalElements();
    const label = this.itemLabel();
    const singular = label.endsWith('s') ? label.slice(0, -1) : label;
    const noun = total === 1 ? singular : label;
    return `${total} ${noun} · Page ${this.page() + 1} of ${this.totalPages()}`;
  });

  protected readonly canPrev = computed(() => this.page() > 0 && !this.loading());
  protected readonly canNext = computed(
    () => (this.page() + 1) * this.pageSize() < this.totalElements() && !this.loading(),
  );
}
