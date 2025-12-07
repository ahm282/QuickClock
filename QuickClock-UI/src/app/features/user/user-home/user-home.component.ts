import { Component } from '@angular/core';
import { UserWelcomeComponent } from "../user-welcome/user-welcome.component";
import { AttendanceScannerComponentComponent } from "../attendance-scanner-component/attendance-scanner-component.component";

@Component({
  selector: 'app-user-home',
  imports: [UserWelcomeComponent, AttendanceScannerComponentComponent],
  templateUrl: './user-home.component.html',
  styleUrl: './user-home.component.css',
})
export class UserHomeComponent {

}
