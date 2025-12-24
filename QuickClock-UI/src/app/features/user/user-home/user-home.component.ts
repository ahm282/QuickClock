import { Component } from '@angular/core';
import { UserWelcomeComponent } from '../user-welcome/user-welcome.component';
import { AttendanceScannerComponent } from '../attendance-scanner/attendance-scanner.component';
import { WorkHoursCardComponent } from '../work-hours-card/work-hours-card.component';

@Component({
    selector: 'app-user-home',
    imports: [
        UserWelcomeComponent,
        AttendanceScannerComponent,
        WorkHoursCardComponent,
    ],
    templateUrl: './user-home.component.html',
    styleUrl: './user-home.component.css',
})
export class UserHomeComponent {}
