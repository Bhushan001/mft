import { Routes } from '@angular/router';

export const CONSOLE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./console-layout.component').then(m => m.ConsoleLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/console-dashboard.component').then(
            m => m.ConsoleDashboardComponent,
          ),
      },
      {
        path: 'tenants',
        loadComponent: () =>
          import('./pages/tenants/tenants.component').then(m => m.TenantsComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./pages/users/users.component').then(m => m.UsersComponent),
      },
    ],
  },
];
