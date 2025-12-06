import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { AppHeaderComponent } from '../../shared/components/app-header/app-header.component';

@Component({
    selector: 'app-main-layout',
    standalone: true,
    imports: [CommonModule, RouterOutlet, RouterLink, AppHeaderComponent],
    templateUrl: './main-layout.component.html',
    styleUrls: ['./main-layout.component.css'],
})
export class MainLayoutComponent {
    authService = inject(AuthService);

    getYear(): number {
        return new Date().getFullYear();
    }

    logout(): void {
        this.authService.logout().subscribe();
    }
}
