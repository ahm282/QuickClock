import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const rolesGuard: CanActivateFn = (route) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const requiredRoles: string[] = route.data['roles'] ?? [];
    const excludedRoles: string[] = route.data['excludeRoles'] ?? [];

    // Check if user has any excluded roles
    if (
        excludedRoles.length > 0 &&
        excludedRoles.some((role) => authService.hasRole(role))
    ) {
        // Kiosk user trying to access employee routes - redirect to kiosk
        if (authService.isKiosk()) {
            return router.createUrlTree(['/kiosk']);
        }
        return router.createUrlTree(['/home']);
    }

    if (requiredRoles.length === 0) {
        return true;
    }

    if (requiredRoles.some((role) => authService.hasRole(role))) {
        return true;
    }

    // User doesn't have required role - redirect to home
    return router.createUrlTree(['/home']);
};
