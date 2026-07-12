import {
  ChangeDetectionStrategy,
  Component,
  signal,
} from '@angular/core';
import type { ICellRendererAngularComp } from 'ag-grid-angular';
import type { ICellRendererParams } from 'ag-grid-community';
import { ManagedUser } from '../../models/user.models';

export interface UserActionsCellContext {
  currentUserId: string | null | undefined;
  onView: (user: ManagedUser) => void;
  onEdit: (user: ManagedUser) => void;
  onToggle: (user: ManagedUser) => void;
  onReset: (user: ManagedUser) => void;
}

type UserActionsParams = ICellRendererParams<ManagedUser, unknown, UserActionsCellContext>;

@Component({
  selector: 'app-user-actions-cell-renderer',
  standalone: true,
  template: `
    @if (user(); as u) {
      <div class="user-grid-actions">
        <button
          type="button"
          class="user-grid-actions__btn"
          [attr.aria-label]="'View user ' + u.username"
          (click)="view()"
        >
          View
        </button>
        <button
          type="button"
          class="user-grid-actions__btn"
          [attr.aria-label]="'Edit user ' + u.username"
          (click)="edit()"
        >
          Edit
        </button>
        @if (!isSelf()) {
          <button
            type="button"
            class="user-grid-actions__btn"
            [attr.aria-label]="
              (u.enabled ? 'Disable' : 'Enable') + ' user ' + u.username
            "
            (click)="toggle()"
          >
            {{ u.enabled ? 'Disable' : 'Enable' }}
          </button>
        }
        <button
          type="button"
          class="user-grid-actions__btn"
          [attr.aria-label]="'Reset password for ' + u.username"
          (click)="reset()"
        >
          Reset password
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      height: 100%;
    }
    .user-grid-actions {
      display: flex;
      height: 100%;
      align-items: center;
      gap: 0.25rem;
    }
    .user-grid-actions__btn {
      border-radius: 0.375rem;
      padding: 0.25rem 0.5rem;
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--color-primary-700, #1d4ed8);
      background: transparent;
      border: none;
      cursor: pointer;
    }
    .user-grid-actions__btn:hover {
      background: var(--color-primary-50, #eff6ff);
    }
    .user-grid-actions__btn:focus-visible {
      outline: 2px solid var(--color-primary-500, #3b82f6);
      outline-offset: 2px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserActionsCellRenderer implements ICellRendererAngularComp {
  private params?: UserActionsParams;

  readonly user = signal<ManagedUser | null>(null);
  readonly isSelf = signal(false);

  agInit(params: UserActionsParams): void {
    this.applyParams(params);
  }

  refresh(params: UserActionsParams): boolean {
    this.applyParams(params);
    return true;
  }

  view(): void {
    const u = this.user();
    if (u) {
      this.params?.context?.onView(u);
    }
  }

  edit(): void {
    const u = this.user();
    if (u) {
      this.params?.context?.onEdit(u);
    }
  }

  toggle(): void {
    const u = this.user();
    if (u && !this.isSelf()) {
      this.params?.context?.onToggle(u);
    }
  }

  reset(): void {
    const u = this.user();
    if (u) {
      this.params?.context?.onReset(u);
    }
  }

  private applyParams(params: UserActionsParams): void {
    this.params = params;
    const data = params.data ?? null;
    this.user.set(data);
    const currentUserId = params.context?.currentUserId;
    this.isSelf.set(!!data && !!currentUserId && data.id === currentUserId);
  }
}
