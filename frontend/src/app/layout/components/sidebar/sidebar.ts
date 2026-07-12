import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideAngularModule,
  LayoutDashboard,
  Users,
  FileText,
  UserCog,
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

  private readonly allItems: NavItem[] = [
    { label: 'Dashboard', path: APP_PATHS.dashboard, icon: LayoutDashboard },
    { label: 'Students', path: APP_PATHS.students, icon: Users },
    { label: 'Reports', path: APP_PATHS.reports, icon: FileText },
    { label: 'Users', path: APP_PATHS.users, icon: UserCog, adminOnly: true },
  ];

  protected readonly navItems = computed(() =>
    this.allItems.filter((item) => !item.adminOnly || this.authService.isAdmin()),
  );
}
