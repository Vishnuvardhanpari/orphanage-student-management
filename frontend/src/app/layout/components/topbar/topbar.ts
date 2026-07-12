import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LucideAngularModule, Menu, Moon, Sun, LogOut } from 'lucide-angular';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../features/auth/services/auth.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Topbar {
  private readonly themeService = inject(ThemeService);
  private readonly authService = inject(AuthService);

  protected readonly appName = environment.appName;
  protected readonly mode = this.themeService.mode;
  protected readonly currentUser = this.authService.currentUser;
  protected readonly icons = { Menu, Moon, Sun, LogOut };

  protected toggleTheme(): void {
    this.themeService.toggle();
  }

  protected logout(): void {
    this.authService.logout().subscribe();
  }
}
