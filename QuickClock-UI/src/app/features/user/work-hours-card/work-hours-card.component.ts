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
    standalone: true,
    imports: [IntlNumberPipe],
    templateUrl: './work-hours-card.component.html',
    styleUrl: './work-hours-card.component.css',
})
export class WorkHoursCardComponent {
    private http = inject(HttpClient);

    workHours: WorkHoursDTO | null = null;
    loading = false;
    error: string | null = null;

    ngOnInit(): void {
        this.fetchWorkingHours();
    }

    fetchWorkingHours(): void {
        this.loading = true;
        this.error = null;

        this.http
            .get<WorkHoursDTO>(`${environment.apiUrl}/clock/hours/me`)
            .subscribe({
                next: (hours) => {
                    this.workHours = hours;
                    this.loading = false;
                },
                error: (err) => {
                    console.error('Failed to fetch work hours:', err);
                    this.error = 'Failed to load work hours.';
                    this.loading = false;
                },
            });
    }
}
