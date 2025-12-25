import { Pipe, PipeTransform, Inject, LOCALE_ID } from '@angular/core';

@Pipe({ name: 'intlNumber', standalone: true })
export class IntlNumberPipe implements PipeTransform {
    constructor(@Inject(LOCALE_ID) private locale: string) {}

    transform(value: number | null | undefined, maxFrac = 2): string {
        if (value == null) return '';
        const locale = this.locale.startsWith('ar')
            ? `${this.locale}-u-nu-arab`
            : this.locale;
        return new Intl.NumberFormat(locale, {
            maximumFractionDigits: maxFrac,
        }).format(value);
    }
}
