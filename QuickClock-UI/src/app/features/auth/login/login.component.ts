import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
    ReactiveFormsModule,
    FormBuilder,
    Validators,
    FormControl,
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LucideAngularModule, Hand } from 'lucide-angular';
import { AppHeaderComponent } from '../../../shared/components/app-header/app-header.component';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        LucideAngularModule,
        AppHeaderComponent,
    ],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css',
})
export class LoginComponent {
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);
    private router = inject(Router);
    readonly hand = Hand;

    isLoading = false;
    errorMsg = '';

    loginForm = this.fb.group({
        username: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required],
        }),
        password: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required],
        }),
    });

    onForgotPassword() {
        this.router.navigate(['/forgot-password']);
    }

    onSubmit() {
        if (this.loginForm.invalid) return;
        this.isLoading = true;
        this.errorMsg = '';
        const { username, password } = this.loginForm.getRawValue();

        this.authService.login({ username, password }).subscribe({
            next: () => {
                this.isLoading = false;
                if (this.authService.isKiosk()) {
                    this.router.navigate(['/kiosk']);
                } else {
                    this.router.navigate(['/home']);
                }
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMsg =
                    err.error?.message ||
                    $localize`:@@login.error.generic:Login failed. Please try again.`;
                console.error(err);
            },
        });
    }

    getYear(): number {
        return new Date().getFullYear();
    }
}
