import { Component, inject, LOCALE_ID, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
    private locale = inject(LOCALE_ID);
    private document = inject(DOCUMENT);

    ngOnInit() {
        const htmlElement = this.document.documentElement;
        const isArabic = this.locale.startsWith('ar');

        htmlElement.setAttribute('lang', this.locale);
        htmlElement.setAttribute('dir', isArabic ? 'rtl' : 'ltr');

        if (isArabic) {
            htmlElement.classList.add('locale-ar');
        }
    }
}
