import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Default redirect
  { path: '', redirectTo: '/hub', pathMatch: 'full' },

  // Auth portal (public — redirect authenticated users away)
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () =>
      import('./portals/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },

  // Hub portal (overview / landing for all authenticated users)
  {
    path: 'hub',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./portals/hub/hub.routes').then(m => m.HUB_ROUTES),
    data: { portal: 'hub' },
  },

  // Console portal (admin — PLATFORM_ADMIN + TENANT_ADMIN)
  {
    path: 'console',
    canActivate: [authGuard, roleGuard],
    data: {
      portal: 'console',
      roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'],
    },
    loadChildren: () =>
      import('./portals/console/console.routes').then(m => m.CONSOLE_ROUTES),
  },

  // Workspace portal (daily operations — all authenticated users)
  {
    path: 'workspace',
    canActivate: [authGuard],
    data: { portal: 'workspace' },
    loadChildren: () =>
      import('./portals/workspace/workspace.routes').then(m => m.WORKSPACE_ROUTES),
  },

  // Unauthorized
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./shared/components/error-pages/unauthorized.component').then(
        m => m.UnauthorizedComponent,
      ),
  },

  // 404
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/error-pages/not-found.component').then(
        m => m.NotFoundComponent,
      ),
  },
];
