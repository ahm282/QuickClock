import { inject } from '@angular/core';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map, catchError, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isAccessTokenValid()) {
        return true;
    }

    return authService.refresh().pipe(
        map(() => true),
        catchError(() => {
            authService.clearAuth();
            return of(router.createUrlTree(['/login']));
        })
    );
};
