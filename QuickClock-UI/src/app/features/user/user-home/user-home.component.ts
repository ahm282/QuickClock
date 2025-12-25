import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UserWelcomeComponent } from '../user-welcome/user-welcome.component';
import { AttendanceScannerComponent } from '../attendance-scanner/attendance-scanner.component';
import { WorkHoursCardComponent } from '../work-hours-card/work-hours-card.component';
import {
    Activity,
    RecentActivityCardComponent,
} from '../../../recent-activity-card/recent-activity-card.component';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-user-home',
    imports: [
        CommonModule,
        UserWelcomeComponent,
        AttendanceScannerComponent,
        WorkHoursCardComponent,
        RecentActivityCardComponent,
    ],
    templateUrl: './user-home.component.html',
    styleUrl: './user-home.component.css',
})
export class UserHomeComponent {
    private http = inject(HttpClient);

    activities: Activity[] = [];
    loading = false;
    error: string | null = null;

    ngOnInit() {
        this.fetchRecentActivities();
    }

    fetchRecentActivities() {
        this.loading = true;
        this.error = null;

        this.http
            .get<Activity[]>(`${environment.apiUrl}/clock/activity/me`)
            .subscribe({
                next: (data) => {
                    this.activities = data;
                    this.loading = false;
                },
                error: (err) => {
                    this.error = 'Failed to load recent activities.';
                    this.loading = false;
                    console.error('Error fetching activities:', err);
                },
            });
    }
}
