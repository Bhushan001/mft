import { Routes } from '@angular/router';

export const HUB_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./hub-layout.component').then(m => m.HubLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/hub-dashboard.component').then(
            m => m.HubDashboardComponent,
          ),
      },
      {
        path: 'audit',
        loadComponent: () =>
          import('./pages/audit/audit-log.component').then(
            m => m.AuditLogComponent,
          ),
      },
    ],
  },
];
