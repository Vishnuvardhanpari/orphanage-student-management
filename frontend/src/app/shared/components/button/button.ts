import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
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

  readonly pressed = output<MouseEvent>();

  protected onClick(event: MouseEvent): void {
    if (!this.disabled()) {
      this.pressed.emit(event);
    }
  }
}
