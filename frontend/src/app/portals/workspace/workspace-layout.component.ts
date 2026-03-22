import { Component, ChangeDetectionStrategy } from '@angular/core';
import { PortalLayoutComponent, NavItem } from '../../shared/components/portal-layout/portal-layout.component';

const WORKSPACE_NAV: NavItem[] = [
  { label: 'Dashboard',  icon: 'dashboard',       route: '/workspace/dashboard' },
  { label: 'Jobs',       icon: 'play_circle_outline', route: '/workspace/jobs' },
  { label: 'Mappings',   icon: 'account_tree',    route: '/workspace/mappings' },
  { label: 'History',    icon: 'history',         route: '/workspace/history' },
];

@Component({
  selector: 'chrono-workspace-layout',
  standalone: true,
  imports: [PortalLayoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <chrono-portal-layout portal="workspace" [navItems]="navItems" />
  `,
})
export class WorkspaceLayoutComponent {
  readonly navItems = WORKSPACE_NAV;
}
