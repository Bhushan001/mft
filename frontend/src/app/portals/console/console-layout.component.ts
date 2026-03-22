import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PortalLayoutComponent, NavItem } from '../../shared/components/portal-layout/portal-layout.component';

const CONSOLE_NAV: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard',            route: '/console/dashboard' },
  { label: 'Tenants',   icon: 'business',             route: '/console/tenants',
    roles: ['PLATFORM_ADMIN'] },
  { label: 'Users',     icon: 'people_outline',       route: '/console/users' },
  { label: 'Settings',  icon: 'settings_outlined',    route: '/console/settings' },
];

@Component({
  selector: 'chrono-console-layout',
  standalone: true,
  imports: [PortalLayoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <chrono-portal-layout portal="console" [navItems]="navItems" />
  `,
})
export class ConsoleLayoutComponent {
  readonly navItems = CONSOLE_NAV;
}
