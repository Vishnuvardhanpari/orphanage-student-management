import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type SkeletonVariant = 'block' | 'text' | 'card' | 'chart' | 'table-row';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  templateUrl: './skeleton.html',
  styleUrl: './skeleton.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block',
    'aria-hidden': 'true',
  },
})
export class Skeleton {
  readonly variant = input<SkeletonVariant>('block');
  /** When true, block/text skeletons use the tall (h-32) size. */
  readonly tall = input(false);
}
