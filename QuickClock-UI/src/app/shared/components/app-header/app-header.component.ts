import { Component, inject } from '@angular/core';
import { LucideAngularModule, Clock, Moon, SunMedium } from 'lucide-angular';
import { LogoutButtonComponent } from '../logout-button/logout-button.component';
import { AppLogoComponent } from '../app-logo/app-logo.component';
import { AuthService } from '../../../core/services/auth.service';

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

    ngOnInit(): void {
        const saved = localStorage.getItem('theme') as
            | 'corporate'
            | 'business'
            | null;
        if (saved === 'corporate' || saved === 'business') {
            this.currentTheme = saved;
        }

        document.documentElement.setAttribute('data-theme', this.currentTheme);
    }

    switchTheme(): void {
        this.currentTheme =
            this.currentTheme === 'corporate' ? 'business' : 'corporate';
        document.documentElement.setAttribute('data-theme', this.currentTheme);
        localStorage.setItem('theme', this.currentTheme);
    }
}
