import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideAngularModule, Clock, Moon, SunMedium } from 'lucide-angular';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, LucideAngularModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css',
})
export class AppComponent {
    readonly clock = Clock;
    readonly sunMedium = SunMedium;
    readonly moon = Moon;

    readonly title = 'Login - QuickClock';
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

    getYear(): number {
        return new Date().getFullYear();
    }
}
