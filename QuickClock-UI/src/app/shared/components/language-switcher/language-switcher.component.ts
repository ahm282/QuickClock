import { Component, Inject, LOCALE_ID } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Check, Languages, LucideAngularModule } from 'lucide-angular';

@Component({
    selector: 'app-language-switcher',
    imports: [LucideAngularModule],
    templateUrl: './language-switcher.component.html',
    styleUrl: './language-switcher.component.css',
})
export class LanguageSwitcherComponent {
    readonly languagesIcon = Languages;
    readonly checkIcon = Check;
    flagEG: SafeHtml;
    flagUK: SafeHtml;

    constructor(
        @Inject(LOCALE_ID) public currentLocale: string,
        private sanitizer: DomSanitizer,
    ) {
        this.flagEG = this.sanitizer.bypassSecurityTrustHtml(`
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" style="width:100%;height:100%;display:block"><mask id="SVGuywqVbel"><circle cx="256" cy="256" r="256" fill="#fff"/></mask><g mask="url(#SVGuywqVbel)"><path fill="#eee" d="m0 144l256-32l256 32v224l-256 32L0 368Z"/><path fill="#d80027" d="M0 0h512v144H0Z"/><path fill="#333" d="M0 368h512v144H0Z"/><path fill="#ff9811" d="M250 191c-8 0-17 4-22 14c5-3 16-1 16 13c0 4-2 8-5 10c-8 0-14-14-29-14c-10 0-19 7-19 17v69l46-7l-14 27h66l-14-27l46 7v-69c0-10-9-17-19-17c-15 0-21 14-29 14c8-23-7-37-23-37"/></g></svg>
        `);

        this.flagUK = this.sanitizer.bypassSecurityTrustHtml(`
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" style="width:100%;height:100%;display:block"><mask id="SVGuywqVbel"><circle cx="256" cy="256" r="256" fill="#fff"/></mask><g mask="url(#SVGuywqVbel)"><path fill="#eee" d="m0 0l8 22l-8 23v23l32 54l-32 54v32l32 48l-32 48v32l32 54l-32 54v68l22-8l23 8h23l54-32l54 32h32l48-32l48 32h32l54-32l54 32h68l-8-22l8-23v-23l-32-54l32-54v-32l-32-48l32-48v-32l-32-54l32-54V0l-22 8l-23-8h-23l-54 32l-54-32h-32l-48 32l-48-32h-32l-54 32L68 0z"/><path fill="#0052b4" d="M336 0v108L444 0Zm176 68L404 176h108zM0 176h108L0 68ZM68 0l108 108V0Zm108 512V404L68 512ZM0 444l108-108H0Zm512-108H404l108 108Zm-68 176L336 404v108z"/><path fill="#d80027" d="M0 0v45l131 131h45zm208 0v208H0v96h208v208h96V304h208v-96H304V0zm259 0L336 131v45L512 0zM176 336L0 512h45l131-131zm160 0l176 176v-45L381 336z"/></g></svg>
        `);
    }

    switchLanguage(targetLocale: string): void {
        if (this.currentLocale === targetLocale) {
            return;
        }

        localStorage.setItem('locale', targetLocale);
        const { pathname, search, hash, origin } = window.location;

        // "/en-US/dashboard" -> ["en-US", "dashboard"]
        const segments = pathname.split('/').filter(Boolean);
        const supportedLocales = ['en-US', 'ar-EG'];

        if (segments.length > 0 && supportedLocales.includes(segments[0])) {
            segments[0] = targetLocale;
        } else {
            segments.unshift(targetLocale);
        }

        // Reconstruct URL
        const newPath = '/' + segments.join('/');
        window.location.href = `${origin}${newPath}${search}${hash}`;
    }
}
