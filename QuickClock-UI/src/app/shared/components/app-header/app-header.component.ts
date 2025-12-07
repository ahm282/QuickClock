import { Component } from '@angular/core';
import { LucideAngularModule, Clock, Moon, SunMedium } from 'lucide-angular';
import { LogoutButtonComponent } from '../logout-button/logout-button.component';

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [LucideAngularModule, LogoutButtonComponent],
    templateUrl: './app-header.component.html',
    styleUrl: './app-header.component.css',
})
export class AppHeaderComponent {
    readonly clock = Clock;
    readonly sunMedium = SunMedium;
    readonly moon = Moon;

    currentTheme: 'light-t' | 'dark-t' = 'light-t';

    ngOnInit(): void {
        const saved = localStorage.getItem('theme') as
            | 'light-t'
            | 'dark-t'
            | null;
        if (saved === 'light-t' || saved === 'dark-t') {
            this.currentTheme = saved;
        }

        document.documentElement.setAttribute('data-theme', this.currentTheme);
    }

    switchTheme(): void {
        this.currentTheme =
            this.currentTheme === 'light-t' ? 'dark-t' : 'light-t';
        document.documentElement.setAttribute('data-theme', this.currentTheme);
        localStorage.setItem('theme', this.currentTheme);
    }
}
