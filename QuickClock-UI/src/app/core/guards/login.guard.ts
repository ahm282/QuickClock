import { CanActivateFn } from '@angular/router';
import { ensureAuthOrRefresh, roleHomeUrlTree } from './guard-helpers';

export const loginGuard: CanActivateFn = () =>
    ensureAuthOrRefresh(
        () => roleHomeUrlTree(), // authenticated => go home
        () => true, // not authenticated => can view login
    );
