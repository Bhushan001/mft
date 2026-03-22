import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PortalLayoutComponent, NavItem } from '../../shared/components/portal-layout/portal-layout.component';

const HUB_NAV: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard',             route: '/hub/dashboard' },
  { label: 'Audit Log', icon: 'history',               route: '/hub/audit' },
  { label: 'Workspace', icon: 'work_outline',          route: '/workspace' },
  { label: 'Console',   icon: 'admin_panel_settings',  route: '/console',
    roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
];

@Component({
  selector: 'chrono-hub-layout',
  standalone: true,
  imports: [PortalLayoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <chrono-portal-layout portal="hub" [navItems]="navItems" />
  `,
})
export class HubLayoutComponent {
  readonly navItems = HUB_NAV;
}
