import { Component } from '@angular/core';
import { LucideAngularModule, Moon, SunMedium } from 'lucide-angular';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-theme-toggle',
    standalone: true,
    imports: [LucideAngularModule],
    templateUrl: './theme-toggle.component.html',
    styleUrls: [],
})
export class ThemeToggleComponent {
    readonly sun = SunMedium;
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
            localStorage.setItem('theme', 'business');
        } else {
            this.currentTheme = 'corporate';
            localStorage.setItem('theme', 'corporate');
        }

        document.documentElement.setAttribute('data-theme', this.currentTheme);
    }

    toggle(): void {
        const newTheme =
            this.currentTheme === 'corporate' ? 'business' : 'corporate';

        document.documentElement.classList.add('theme-transition');

        requestAnimationFrame(() => {
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            this.currentTheme = newTheme;

            window.setTimeout(() => {
                document.documentElement.classList.remove('theme-transition');
            }, this.THEME_TRANSITION_MS);
        });
    }
}
