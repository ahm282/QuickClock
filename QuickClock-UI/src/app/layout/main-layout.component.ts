import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-main-layout',
    standalone: true,
    imports: [CommonModule, RouterOutlet, RouterLink],
    templateUrl: './main-layout.component.html',
    styleUrls: ['./main-layout.component.css'],
})
export class MainLayoutComponent {
    authService = inject(AuthService);

    logout(): void {
        this.authService.logout().subscribe();
    }
}
