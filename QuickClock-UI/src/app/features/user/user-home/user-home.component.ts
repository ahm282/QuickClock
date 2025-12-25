import { Component, inject } from '@angular/core';
import { UserWelcomeComponent } from '../user-welcome/user-welcome.component';
import { AttendanceScannerComponent } from '../attendance-scanner/attendance-scanner.component';
import { WorkHoursCardComponent } from '../work-hours-card/work-hours-card.component';
import { RecentActivityCardComponent } from '../recent-activity-card/recent-activity-card.component';
import { CommonModule } from '@angular/common';
import { ClockService } from '../../../core/services/clock.service';

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
    clockService = inject(ClockService);

    ngOnInit() {
        // Initialize all clock-related data
        this.clockService.initializeData();
    }
}
