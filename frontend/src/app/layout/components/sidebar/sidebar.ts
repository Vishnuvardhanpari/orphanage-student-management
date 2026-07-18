import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideAngularModule,
  LayoutDashboard,
  Users,
  FileText,
  UserCog,
  Archive,
  ScrollText,
} from 'lucide-angular';
import { APP_PATHS } from '../../../core/constants/routes';
import { AuthService } from '../../../features/auth/services/auth.service';

interface NavItem {
  label: string;
  path: string;
  icon: typeof LayoutDashboard;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sidebar {
  private readonly authService = inject(AuthService);

  protected readonly paths = APP_PATHS;

  private readonly allItems: NavItem[] = [
    { label: 'Dashboard', path: APP_PATHS.dashboard, icon: LayoutDashboard },
    { label: 'Students', path: APP_PATHS.students, icon: Users },
    { label: 'Archived Students', path: APP_PATHS.studentsInactive, icon: Archive },
    { label: 'Reports', path: APP_PATHS.reports, icon: FileText },
    { label: 'Users', path: APP_PATHS.users, icon: UserCog, adminOnly: true },
    { label: 'Audit Logs', path: APP_PATHS.audit, icon: ScrollText, adminOnly: true },
  ];

  protected readonly navItems = computed(() =>
    this.allItems.filter((item) => !item.adminOnly || this.authService.isAdmin()),
  );
}
