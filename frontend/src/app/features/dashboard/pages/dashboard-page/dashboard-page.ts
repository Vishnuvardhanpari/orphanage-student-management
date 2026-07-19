import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import { Subject, catchError, forkJoin, of, switchMap } from 'rxjs';
import { APP_PATHS } from '../../../../core/constants/routes';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Skeleton } from '../../../../shared/components/skeleton/skeleton';
import { echarts } from '../../echarts.config';
import {
  DashboardGenderCount,
  DashboardMonthlyAdmission,
  DashboardRecentStudent,
  DashboardStatusCount,
  DashboardSummary,
  studentDisplayName,
} from '../../models/dashboard.models';
import { DashboardService } from '../../services/dashboard.service';

interface StatCard {
  label: string;
  value: number;
  hint: string;
}

interface ChartSlice {
  name: string;
  value: number;
}

interface DashboardPayload {
  summary: DashboardSummary;
  admissions: DashboardMonthlyAdmission[];
  gender: DashboardGenderCount[];
  status: DashboardStatusCount[];
}

const CHART_COLOR_FALLBACKS = {
  primary: '#2563eb',
  warning: '#ea580c',
  inactive: '#6b7280',
} as const;

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    PageHeader,
    Button,
    EmptyState,
    Skeleton,
    RouterLink,
    DatePipe,
    NgxEchartsDirective,
  ],
  providers: [provideEchartsCore({ echarts })],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();

  protected readonly paths = APP_PATHS;
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly admissions = signal<DashboardMonthlyAdmission[]>([]);
  protected readonly gender = signal<DashboardGenderCount[]>([]);
  protected readonly status = signal<DashboardStatusCount[]>([]);

  protected readonly displayName = studentDisplayName;

  protected readonly statCards = computed<StatCard[]>(() => {
    const s = this.summary();
    if (!s) {
      return [];
    }
    return [
      { label: 'Total Students', value: s.totalStudents, hint: 'Active + left' },
      { label: 'Active Students', value: s.activeStudents, hint: 'Current roster' },
      { label: 'Left Students', value: s.inactiveStudents, hint: 'Archived records' },
      { label: 'New Admissions', value: s.newAdmissions, hint: 'This month (UTC)' },
      { label: 'Male Students', value: s.maleStudents, hint: 'Active roster' },
      { label: 'Female Students', value: s.femaleStudents, hint: 'Active roster' },
    ];
  });

  protected readonly genderSlices = computed<ChartSlice[]>(() =>
    this.gender().map((row) => ({
      name: this.genderLabel(row.gender),
      value: row.count,
    })),
  );

  protected readonly statusSlices = computed<ChartSlice[]>(() =>
    this.status().map((row) => ({
      name: row.status === 'ACTIVE' ? 'Active' : 'Left',
      value: row.count,
    })),
  );

  protected readonly genderChartSummary = computed(() =>
    this.formatSliceSummary(this.genderSlices()),
  );

  protected readonly statusChartSummary = computed(() =>
    this.formatSliceSummary(this.statusSlices()),
  );

  protected readonly admissionsChartSummary = computed(() => {
    const series = this.admissions();
    if (series.length === 0) {
      return 'No monthly admission data.';
    }
    return series.map((row) => `${row.yearMonth}: ${row.count}`).join(', ');
  });

  protected readonly genderChartOption = computed<EChartsCoreOption>(() =>
    this.buildPieOption('Gender', this.genderSlices()),
  );

  protected readonly statusChartOption = computed<EChartsCoreOption>(() =>
    this.buildPieOption('Status', this.statusSlices()),
  );

  protected readonly admissionsChartOption = computed<EChartsCoreOption>(() => {
    const series = this.admissions();
    const colors = this.resolveChartColors();
    return {
      color: [colors.primary],
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 24, bottom: 48 },
      xAxis: {
        type: 'category',
        data: series.map((row) => row.yearMonth),
        axisLabel: { rotate: 45, fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
      },
      series: [
        {
          type: 'bar',
          name: 'Admissions',
          data: series.map((row) => row.count),
          barMaxWidth: 28,
        },
      ],
    };
  });

  ngOnInit(): void {
    this.reload$
      .pipe(
        switchMap(() => {
          this.loading.set(true);
          this.loadError.set(false);
          return forkJoin({
            summary: this.dashboardService.getSummary(),
            admissions: this.dashboardService.getAdmissions(),
            gender: this.dashboardService.getGender(),
            status: this.dashboardService.getStatus(),
          }).pipe(
            catchError(() => {
              this.loadError.set(true);
              this.loading.set(false);
              return of<DashboardPayload | null>(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((payload) => {
        if (!payload) {
          return;
        }
        this.summary.set(payload.summary);
        this.admissions.set(payload.admissions);
        this.gender.set(payload.gender);
        this.status.set(payload.status);
        this.loading.set(false);
      });

    this.reload$.next();
  }

  protected refresh(): void {
    this.reload$.next();
  }

  protected studentProfileLink(student: DashboardRecentStudent): string {
    return `${APP_PATHS.students}/${student.id}`;
  }

  private buildPieOption(name: string, data: ChartSlice[]): EChartsCoreOption {
    const hasData = data.some((row) => row.value > 0);
    const colors = this.resolveChartColors();
    return {
      color: [colors.primary, colors.warning, colors.inactive],
      tooltip: { trigger: 'item' },
      legend: {
        bottom: 0,
        left: 'center',
      },
      series: [
        {
          name,
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          label: { formatter: '{b}: {c}' },
          data: hasData ? data : [{ name: 'No data', value: 0 }],
        },
      ],
    };
  }

  private formatSliceSummary(slices: ChartSlice[]): string {
    if (slices.length === 0) {
      return 'No data.';
    }
    return slices.map((slice) => `${slice.name}: ${slice.value}`).join(', ');
  }

  /**
   * Reads OMS theme tokens for ECharts. Falls back when CSS vars are unavailable
   * (e.g. unit tests without the full stylesheet).
   */
  private resolveChartColors(): {
    primary: string;
    warning: string;
    inactive: string;
  } {
    if (typeof document === 'undefined') {
      return { ...CHART_COLOR_FALLBACKS };
    }
    const styles = getComputedStyle(document.documentElement);
    return {
      primary: this.readCssColor(styles, '--color-primary-600', CHART_COLOR_FALLBACKS.primary),
      warning: this.readCssColor(styles, '--color-warning-600', CHART_COLOR_FALLBACKS.warning),
      inactive: this.readCssColor(styles, '--color-inactive-500', CHART_COLOR_FALLBACKS.inactive),
    };
  }

  private readCssColor(
    styles: CSSStyleDeclaration,
    property: string,
    fallback: string,
  ): string {
    const value = styles.getPropertyValue(property).trim();
    return value.length > 0 ? value : fallback;
  }

  private genderLabel(gender: DashboardGenderCount['gender']): string {
    switch (gender) {
      case 'MALE':
        return 'Male';
      case 'FEMALE':
        return 'Female';
      default:
        return 'Other';
    }
  }
}
