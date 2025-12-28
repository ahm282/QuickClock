import { inject } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export type GuardResult = boolean | UrlTree | Observable<boolean | UrlTree>;

export function roleHomeUrlTree(): UrlTree {
    const auth = inject(AuthService);
    const router = inject(Router);

    return auth.isKiosk()
        ? router.createUrlTree(['/kiosk'])
        : router.createUrlTree(['/home']);
}

/**
 * Ensures auth is valid (token or refresh). If target is a "login" route, you likely
 * want to redirect authenticated users to home; otherwise allow.
 *
 * @param onAuthed - what to do if authenticated (e.g. redirect or allow)
 * @param onUnauthed - what to do if not authenticated and refresh fails
 */
export function ensureAuthOrRefresh(
    onAuthed: () => boolean | UrlTree,
    onUnauthed: () => boolean | UrlTree,
): GuardResult {
    const auth = inject(AuthService);

    if (auth.isAccessTokenValid()) {
        return onAuthed();
    }

    return auth.refresh().pipe(
        map(() => onAuthed()),
        catchError(() => of(onUnauthed())),
    );
}
