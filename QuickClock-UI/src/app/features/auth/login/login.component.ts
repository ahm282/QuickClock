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

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
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
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMsg =
                    err.error?.message || 'Login failed. Please try again.';
                console.error(err);
            },
        });
    }
}
