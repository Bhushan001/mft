import {
  Component,
  ChangeDetectionStrategy,
  Input,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
  badge?: number;
}

@Component({
  selector: 'chrono-portal-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="app-shell" [attr.data-portal]="portal">
      <!-- Sidebar -->
      <aside class="sidebar" [class.collapsed]="sidebarCollapsed()">
        <!-- Brand -->
        <a routerLink="/" class="sidebar__brand">
          <span class="material-icons-outlined sidebar__logo" style="font-size:28px;color:var(--color-primary)">
            schedule
          </span>
          <span class="sidebar__logo-text">Chrono</span>
        </a>

        <!-- Nav -->
        <nav class="sidebar__nav" role="navigation">
          @for (item of visibleNavItems(); track item.route) {
            <a
              [routerLink]="item.route"
              routerLinkActive="active"
              class="sidebar__item"
              [title]="item.label"
            >
              <span class="sidebar__item-icon">
                <span class="material-icons-outlined">{{ item.icon }}</span>
              </span>
              <span class="sidebar__label">{{ item.label }}</span>
              @if (item.badge) {
                <span class="sidebar__badge">{{ item.badge }}</span>
              }
            </a>
          }
        </nav>

        <!-- Footer -->
        <div class="sidebar__footer">
          <button
            class="sidebar__toggle"
            (click)="sidebarCollapsed.set(!sidebarCollapsed())"
            [title]="sidebarCollapsed() ? 'Expand sidebar' : 'Collapse sidebar'"
          >
            <span class="material-icons-outlined">
              {{ sidebarCollapsed() ? 'chevron_right' : 'chevron_left' }}
            </span>
          </button>
        </div>
      </aside>

      <!-- Main content -->
      <div class="app-shell__main">
        <!-- Header -->
        <header class="app-header">
          <div class="app-header__left">
            <h1 style="font-size:var(--font-size-md);font-weight:var(--font-weight-semibold);margin:0">
              {{ portalTitle }}
            </h1>
          </div>
          <div class="app-header__spacer"></div>
          <div class="app-header__right">
            <!-- Notifications -->
            <button class="app-header__action" title="Notifications">
              <span class="material-icons-outlined">notifications_none</span>
            </button>

            <!-- User menu -->
            <div class="user-avatar" role="button">
              <div class="user-avatar__initials">
                {{ userInitials() }}
              </div>
              <div class="user-avatar__info">
                <span class="user-avatar__name">{{ userName() }}</span>
                <span class="user-avatar__role">{{ userRole() }}</span>
              </div>
            </div>

            <!-- Logout -->
            <button
              class="app-header__action"
              title="Sign out"
              (click)="logout()"
            >
              <span class="material-icons-outlined">logout</span>
            </button>
          </div>
        </header>

        <!-- Page content -->
        <main class="app-shell__content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styleUrl: './portal-layout.component.scss',
})
export class PortalLayoutComponent implements OnInit {
  @Input() portal: 'hub' | 'console' | 'workspace' = 'hub';
  @Input() navItems: NavItem[] = [];

  protected readonly authService = inject(AuthService);
  readonly sidebarCollapsed = signal(false);

  get portalTitle(): string {
    const titles: Record<string, string> = {
      hub: 'Hub',
      console: 'Admin Console',
      workspace: 'Workspace',
    };
    return titles[this.portal] ?? 'Chrono';
  }

  visibleNavItems() {
    const user = this.authService.currentUser();
    if (!user) return [];
    return this.navItems.filter(item => {
      if (!item.roles || item.roles.length === 0) return true;
      return item.roles.includes(user.role);
    });
  }

  userInitials(): string {
    const user = this.authService.currentUser();
    if (!user) return 'U';
    return `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();
  }

  userName(): string {
    const user = this.authService.currentUser();
    if (!user) return '';
    return `${user.firstName} ${user.lastName}`;
  }

  userRole(): string {
    const role = this.authService.currentUser()?.role ?? '';
    const labels: Record<string, string> = {
      PLATFORM_ADMIN: 'Platform Admin',
      TENANT_ADMIN: 'Tenant Admin',
      TENANT_USER: 'User',
    };
    return labels[role] ?? role;
  }

  logout(): void {
    this.authService.logout();
  }

  ngOnInit(): void {
    // Set portal theme on document root
    document.documentElement.setAttribute('data-portal', this.portal);
  }
}
