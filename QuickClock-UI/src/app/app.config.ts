import {
    ApplicationConfig,
    inject,
    LOCALE_ID,
    provideAppInitializer,
    provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';

function resolveLocale(): string {
    return (
        localStorage.getItem('locale') ||
        navigator.language ||
        'en'
    ).trim();
}

let interceptors = [authInterceptor];
export const appConfig: ApplicationConfig = {
    providers: [
        { provide: LOCALE_ID, useFactory: resolveLocale },
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(routes),
        provideHttpClient(withInterceptors(interceptors)),
        provideAppInitializer(() => {
            const authService = inject(AuthService);
            return firstValueFrom(authService.initSession());
        }),
    ],
};
