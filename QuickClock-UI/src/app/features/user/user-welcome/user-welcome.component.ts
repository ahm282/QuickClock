import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'app-user-welcome',
    imports: [DatePipe],
    templateUrl: './user-welcome.component.html',
    styleUrl: './user-welcome.component.css',
})
export class UserWelcomeComponent {
    authService = inject(AuthService);
    today = signal<Date>(new Date());

    timeBasedGreeting(): string {
        const currentHour = new Date().getHours();
        if (currentHour < 12) {
            return 'Good Morning';
        } else if (currentHour < 18) {
            return 'Good Afternoon';
        } else {
            return 'Good Evening';
        }
    }
}
