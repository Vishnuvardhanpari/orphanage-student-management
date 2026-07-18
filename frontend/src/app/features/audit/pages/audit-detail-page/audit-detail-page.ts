import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { NotificationService } from '../../../../core/services/notification.service';
import { Button } from '../../../../shared/components/button/button';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AuditLog } from '../../models/audit.models';
import { AuditService } from '../../services/audit.service';

@Component({
  selector: 'app-audit-detail-page',
  standalone: true,
  imports: [PageHeader, Button, DatePipe],
  templateUrl: './audit-detail-page.html',
  styleUrl: './audit-detail-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auditService = inject(AuditService);
  private readonly notifications = inject(NotificationService);

  readonly paths = APP_PATHS;
  readonly loading = signal(true);
  readonly log = signal<AuditLog | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigateByUrl(APP_PATHS.audit);
      return;
    }
    this.load(id);
  }

  private load(id: string): void {
    this.loading.set(true);
    this.auditService
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (log) => this.log.set(log),
        error: (err: HttpErrorResponse) => {
          this.notifications.error(
            err?.error?.message || 'Failed to load audit log.',
          );
          void this.router.navigateByUrl(APP_PATHS.audit);
        },
      });
  }
}
