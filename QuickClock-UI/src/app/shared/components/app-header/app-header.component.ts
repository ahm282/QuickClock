import { Component, inject } from '@angular/core';
import { LucideAngularModule, Clock, Moon, SunMedium } from 'lucide-angular';
import { LogoutButtonComponent } from '../logout-button/logout-button.component';
import { AppLogoComponent } from '../app-logo/app-logo.component';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [LucideAngularModule, LogoutButtonComponent, AppLogoComponent],
    templateUrl: './app-header.component.html',
    styleUrl: './app-header.component.css',
})
export class AppHeaderComponent {
    authService = inject(AuthService);

    readonly clock = Clock;
    readonly sunMedium = SunMedium;
    readonly moon = Moon;

    currentTheme: 'corporate' | 'business' = 'corporate';
    private readonly THEME_TRANSITION_MS = environment.THEME_TRANSITION_MS;

    ngOnInit(): void {
        const saved = localStorage.getItem('theme') as
            | 'corporate'
            | 'business'
            | null;
        if (saved) {
            this.currentTheme = saved;
        } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
            this.currentTheme = 'business';
        }

        document.documentElement.setAttribute('data-theme', this.currentTheme);
    }

    switchTheme(): void {
        const newTheme =
            this.currentTheme === 'corporate' ? 'business' : 'corporate';

        // Add a temporary class to enable smooth transitions
        document.documentElement.classList.add('theme-transition');

        // Apply new theme
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);

        window.setTimeout(() => {
            document.documentElement.classList.remove('theme-transition');
        }, this.THEME_TRANSITION_MS);

        this.currentTheme = newTheme;
    }
}
