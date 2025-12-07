import { Component } from '@angular/core';
import { UserWelcomeComponent } from "../user-welcome/user-welcome.component";

@Component({
  selector: 'app-user-home',
  imports: [UserWelcomeComponent],
  templateUrl: './user-home.component.html',
  styleUrl: './user-home.component.css',
})
export class UserHomeComponent {

}
