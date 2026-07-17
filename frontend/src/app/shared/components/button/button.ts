import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

/** Same shape RouterLink accepts, re-exported so callers don't need the router import. */
export type ButtonRouterLink = string | readonly unknown[];

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [RouterLink, NgTemplateOutlet],
  templateUrl: './button.html',
  styleUrl: './button.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Button {
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly disabled = input(false);
  readonly fullWidth = input(false);
  /**
   * When set, renders as a native anchor styled like a button instead of a
   * <button> element (Milestone 9 QA — BUG-004). Use this for navigation
   * actions so callers never need to wrap app-button in a separate <a>,
   * which produces invalid nested interactive controls.
   */
  readonly routerLink = input<ButtonRouterLink | null>(null);

  readonly pressed = output<MouseEvent>();

  protected onClick(event: MouseEvent): void {
    if (this.disabled()) {
      if (this.routerLink() !== null) {
        // Anchors have no native disabled state; block both the click and
        // the router navigation it would otherwise trigger.
        event.preventDefault();
        event.stopPropagation();
      }
      return;
    }
    this.pressed.emit(event);
  }
}
