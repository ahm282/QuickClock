import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ensureAuthOrRefresh, roleHomeUrlTree } from './guard-helpers';

export const authGuard: CanActivateFn = (route) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const isRoot =
        route.routeConfig?.path === '' &&
        route.routeConfig?.pathMatch === 'full';

    return ensureAuthOrRefresh(
        () => (isRoot ? roleHomeUrlTree() : true),
        () => {
            auth.clearAuth();
            return router.createUrlTree(['/login']);
        },
    );
};
