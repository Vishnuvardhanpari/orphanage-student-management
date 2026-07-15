import {
  ChangeDetectionStrategy,
  Component,
  signal,
} from '@angular/core';
import type { ICellRendererAngularComp } from 'ag-grid-angular';
import type { ICellRendererParams } from 'ag-grid-community';
import { StudentSummary } from '../../models/student.models';

export interface StudentActionsCellContext {
  onView: (student: StudentSummary) => void;
  onEdit: (student: StudentSummary) => void;
}

type StudentActionsParams = ICellRendererParams<
  StudentSummary,
  unknown,
  StudentActionsCellContext
>;

@Component({
  selector: 'app-student-actions-cell-renderer',
  standalone: true,
  template: `
    @if (student(); as s) {
      <div class="student-grid-actions">
        <button
          type="button"
          class="student-grid-actions__btn"
          [attr.aria-label]="'View student ' + displayName(s)"
          (click)="view()"
        >
          View
        </button>
        <button
          type="button"
          class="student-grid-actions__btn"
          [attr.aria-label]="'Edit student ' + displayName(s)"
          (click)="edit()"
        >
          Edit
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      height: 100%;
    }
    .student-grid-actions {
      display: flex;
      height: 100%;
      align-items: center;
      gap: 0.25rem;
    }
    .student-grid-actions__btn {
      border-radius: 0.375rem;
      padding: 0.25rem 0.5rem;
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--color-primary-700, #1d4ed8);
      background: transparent;
      border: none;
      cursor: pointer;
    }
    .student-grid-actions__btn:hover {
      background: var(--color-primary-50, #eff6ff);
    }
    .student-grid-actions__btn:focus-visible {
      outline: 2px solid var(--color-primary-500, #3b82f6);
      outline-offset: 2px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentActionsCellRenderer implements ICellRendererAngularComp {
  private params?: StudentActionsParams;

  readonly student = signal<StudentSummary | null>(null);

  agInit(params: StudentActionsParams): void {
    this.applyParams(params);
  }

  refresh(params: StudentActionsParams): boolean {
    this.applyParams(params);
    return true;
  }

  displayName(student: StudentSummary): string {
    return [student.firstName, student.lastName].filter(Boolean).join(' ');
  }

  view(): void {
    const s = this.student();
    if (s) {
      this.params?.context?.onView(s);
    }
  }

  edit(): void {
    const s = this.student();
    if (s) {
      this.params?.context?.onEdit(s);
    }
  }

  private applyParams(params: StudentActionsParams): void {
    this.params = params;
    this.student.set(params.data ?? null);
  }
}
