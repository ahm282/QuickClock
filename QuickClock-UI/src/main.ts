/// <reference types="@angular/localize" />

import { bootstrapApplication } from '@angular/platform-browser';
import { registerLocaleData } from '@angular/common';

import localeEn from '@angular/common/locales/en';
import localeNl from '@angular/common/locales/nl';
import localeNlBe from '@angular/common/locales/nl-BE';
import localeFr from '@angular/common/locales/fr';
import localeFrBe from '@angular/common/locales/fr-BE';
import localeAr from '@angular/common/locales/ar';
import localeArEg from '@angular/common/locales/ar-EG';

import '@fontsource-variable/geist';
import '@fontsource-variable/geist-mono';
import '@fontsource/almarai';

import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

function resolveLocale(): string {
    const path = window.location.pathname;

    if (path.indexOf('/ar-EG') === 0) {
        return 'ar-EG';
    }
    if (path.indexOf('/en-US') === 0) {
        return 'en-US';
    }

    // 2. Fallback to storage/browser only if path is ambiguous (e.g. root '/')
    return (
        localStorage.getItem('locale') ||
        navigator.language ||
        'en-US'
    ).trim();
}

function isRtl(locale: string): boolean {
    return /^ar|^fa|^he|^ur/i.test(locale);
}

const locale = resolveLocale();

registerLocaleData(localeEn, 'en-US');
registerLocaleData(localeAr, 'ar');
registerLocaleData(localeArEg, 'ar-EG');

document.documentElement.lang = locale;
document.documentElement.dir = isRtl(locale) ? 'rtl' : 'ltr';

bootstrapApplication(AppComponent, appConfig).catch(console.error);
