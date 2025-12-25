import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LucideAngularModule, LogOut } from 'lucide-angular';

@Component({
    selector: 'app-logout-button',
    standalone: true,
    imports: [LucideAngularModule],
    templateUrl: './logout-button.component.html',
    styleUrl: './logout-button.component.css',
})
export class LogoutButtonComponent {
    private authService = inject(AuthService);
    private router = inject(Router);

    readonly LogOut = LogOut;

    openModal() {
        const modal = document.getElementById(
            'logout_modal',
        ) as HTMLDialogElement;
        if (modal) {
            modal.showModal();
        }
    }

    closeModal() {
        const modal = document.getElementById(
            'logout_modal',
        ) as HTMLDialogElement;
        if (modal) {
            modal.close();
        }
    }

    confirmLogout() {
        this.authService.logout().subscribe({
            next: () => {
                this.closeModal();
                this.router.navigate(['/login']);
            },
            error: () => {
                // Even if the API call fails, clear local auth and redirect
                this.authService.clearAuth();
                this.closeModal();
                this.router.navigate(['/login']);
            },
        });
    }
}
