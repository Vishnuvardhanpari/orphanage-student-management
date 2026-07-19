import { DialogRef } from '@angular/cdk/dialog';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
} from '@angular/core';

/**
 * Shared glass dialog chrome for CDK dialogs.
 * Project title into [dialogTitle], body as default content, actions into [dialogActions].
 * CDK owns role="dialog"; this shell only provides visual structure and applies aria-label
 * to the CDK container so assistive tech gets a single named dialog.
 */
@Component({
  selector: 'app-dialog-shell',
  standalone: true,
  templateUrl: './dialog-shell.html',
  styleUrl: './dialog-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogShell implements AfterViewInit {
  readonly ariaLabel = input.required<string>();
  readonly size = input<'md' | 'lg'>('md');

  private readonly dialogRef = inject(DialogRef, { optional: true });

  ngAfterViewInit(): void {
    const label = this.ariaLabel();
    if (!label || !this.dialogRef) {
      return;
    }
    const host = this.dialogRef.overlayRef?.hostElement;
    if (!host) {
      return;
    }
    const container =
      host.querySelector<HTMLElement>('.cdk-dialog-container') ??
      host.querySelector<HTMLElement>('[role="dialog"]');
    container?.setAttribute('aria-label', label);
  }
}
