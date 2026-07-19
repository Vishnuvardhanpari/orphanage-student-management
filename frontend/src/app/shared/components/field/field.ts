import {
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChild,
  input,
} from '@angular/core';
import { Input } from '../input/input';
import { Select } from '../select/select';
import { Textarea } from '../textarea/textarea';

/** Label + optional hint/error wrapper for form controls. */
@Component({
  selector: 'app-field',
  standalone: true,
  templateUrl: './field.html',
  styleUrl: './field.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block',
  },
})
export class Field {
  readonly label = input('');
  readonly hint = input('');
  readonly error = input('');
  /** Manual override; when empty, links to the projected Input/Select/Textarea id. */
  readonly forId = input('');

  private readonly inputControl = contentChild(Input);
  private readonly selectControl = contentChild(Select);
  private readonly textareaControl = contentChild(Textarea);

  protected readonly resolvedForId = computed(() => {
    const manual = this.forId().trim();
    if (manual) {
      return manual;
    }
    return (
      this.inputControl()?.id() ??
      this.selectControl()?.id() ??
      this.textareaControl()?.id() ??
      ''
    );
  });
}
