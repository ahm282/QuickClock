import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { IntlNumberPipe } from '../../../core/pipes/intl-number.pipe';

interface WorkHoursDTO {
    hoursToday: number;
    hoursThisWeek: number;
}

@Component({
    selector: 'app-work-hours-card',
    imports: [IntlNumberPipe],
    templateUrl: './work-hours-card.component.html',
    styleUrl: './work-hours-card.component.css',
})
export class WorkHoursCardComponent {
    workHours: WorkHoursDTO | null = null;

    private http = inject(HttpClient);

    ngOnInit(): void {
        this.fetchWorkingHours();
    }

    private fetchWorkingHours(): void {
        this.http
            .get<WorkHoursDTO>(`${environment.apiUrl}/clock/hours/me`)
            .subscribe({
                next: (hours) => {
                    this.workHours = hours;
                },
                error: (error) => {
                    console.error('Failed to fetch work hours:', error);
                },
            });
    }
}
