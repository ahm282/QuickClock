import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { DatePipe } from '@angular/common';
import { LocalizedNamePipe } from '../../../core/pipes/localized-name.pipe';

@Component({
    selector: 'app-user-welcome',
    imports: [DatePipe, LocalizedNamePipe],
    templateUrl: './user-welcome.component.html',
    styleUrl: './user-welcome.component.css',
})
export class UserWelcomeComponent {
    authService = inject(AuthService);
    currentUser = this.authService.currentUser;
    today = signal<Date>(new Date());

    timeBasedGreeting(): string {
        const currentHour = new Date().getHours();

        if (currentHour < 12) {
            return $localize`:@@greetingMorning:Good Morning`;
        } else if (currentHour < 18) {
            return $localize`:@@greetingAfternoon:Good Afternoon`;
        } else {
            return $localize`:@@greetingEvening:Good Evening`;
        }
    }
}
