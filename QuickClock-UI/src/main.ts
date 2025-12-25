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

import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

function resolveLocale(): string {
    return (
        localStorage.getItem('locale') ||
        navigator.language ||
        'en'
    ).trim();
}

function isRtl(locale: string): boolean {
    return /^ar|^fa|^he|^ur/i.test(locale);
}

const locale = resolveLocale();

registerLocaleData(localeEn, 'en');

registerLocaleData(localeNl, 'nl');
registerLocaleData(localeNlBe, 'nl-BE');

registerLocaleData(localeFr, 'fr');
registerLocaleData(localeFrBe, 'fr-BE');

registerLocaleData(localeAr, 'ar');
registerLocaleData(localeArEg, 'ar-EG');

document.documentElement.lang = locale;
document.documentElement.dir = isRtl(locale) ? 'rtl' : 'ltr';

bootstrapApplication(AppComponent, appConfig).catch(console.error);
