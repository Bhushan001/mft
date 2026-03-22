import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.models';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles: UserRole[] = route.data['roles'] ?? [];

  if (requiredRoles.length === 0) {
    return true;
  }

  const user = authService.currentUser();
  if (!user) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (requiredRoles.includes(user.role)) {
    return true;
  }

  router.navigate(['/unauthorized']);
  return false;
};
