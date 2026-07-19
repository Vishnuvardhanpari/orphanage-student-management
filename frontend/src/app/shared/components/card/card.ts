import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type CardVariant = 'elevated' | 'glass' | 'muted';
export type CardPadding = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './card.html',
  styleUrl: './card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block',
  },
})
export class Card {
  readonly variant = input<CardVariant>('elevated');
  readonly padding = input<CardPadding>('md');
  /** Soft hover lift for interactive summary surfaces. */
  readonly interactive = input(false);
}
