import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const rolesGuard: CanActivateFn = (route) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const requiredRoles: string[] = route.data['roles'] ?? [];

    if (requiredRoles.length === 0) {
        return true; // No specific roles required
    }

    if (requiredRoles.some((role) => authService.hasRole(role))) {
        return true; // User has at least one of the required roles
    }

    return router.createUrlTree(['/dashboard']);
};
