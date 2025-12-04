import { Component, inject } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-logout',
    imports: [],
    templateUrl: './logout.component.html',
    styleUrl: './logout.component.css',
})
export class LogoutComponent {
    private authService = inject(AuthService);

    ngOnInit(): void {
        this.authService.logout().subscribe();
    }
}
