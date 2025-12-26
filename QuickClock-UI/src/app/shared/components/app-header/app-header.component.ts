import { Component, inject } from '@angular/core';
import { LucideAngularModule, Clock, Moon, SunMedium } from 'lucide-angular';
import { LogoutButtonComponent } from '../logout-button/logout-button.component';
import { AppLogoComponent } from '../app-logo/app-logo.component';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [
        LucideAngularModule,
        LogoutButtonComponent,
        AppLogoComponent,
        ThemeToggleComponent,
    ],
    templateUrl: './app-header.component.html',
    styleUrl: './app-header.component.css',
})
export class AppHeaderComponent {
    authService = inject(AuthService);

    readonly clock = Clock;
    readonly sunMedium = SunMedium;
    readonly moon = Moon;
}
