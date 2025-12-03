import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { catchError, Observable, of, tap, throwError } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    inviteCode: string;
}

export interface AccessTokenResponse {
    accessToken: string;
}

@Injectable({
    providedIn: 'root',
})
export class AuthService {
    private http = inject(HttpClient);
    private router = inject(Router);

    // State
    private accessTokenSignal = signal<string | null>(null);
    public accessToken = this.accessTokenSignal.asReadonly();

    // Expiry time (unix epoch)
    private tokenExpirySignal = signal<number | null>(null);

    // Computed: Is token present and valid?
    public isAccessTokenValid = computed((): boolean => {
        const token = this.accessTokenSignal();
        const expiry = this.tokenExpirySignal();

        if (!token || !expiry) {
            return false;
        }

        return Date.now() / 1000 < expiry; // Compare in seconds
    });

    login(credentials: LoginRequest): Observable<AccessTokenResponse> {
        return this.http
            .post<AccessTokenResponse>(
                `${environment.apiUrl}/auth/login`,
                credentials
            )
            .pipe(
                tap({
                    next: (response) => {
                        this.setSession(response.accessToken);
                    },
                }),
                catchError((error) => {
                    this.clearAuth();
                    return throwError(() => error);
                })
            );
    }

    refresh(): Observable<AccessTokenResponse> {
        return this.http
            .post<AccessTokenResponse>(
                `${environment.apiUrl}/auth/refresh`,
                {},
                { withCredentials: true }
            )
            .pipe(
                tap({
                    next: (response) => {
                        this.setSession(response.accessToken);
                    },
                }),
                catchError((error) => {
                    this.clearAuth();
                    return throwError(() => error);
                })
            );
    }

    logout(): Observable<void> {
        return this.http
            .post<void>(
                `${environment.apiUrl}/auth/logout`,
                {},
                { withCredentials: true }
            )
            .pipe(
                tap({
                    next: () => {
                        this.doLogout();
                    },
                }),
                catchError(() => {
                    this.doLogout();
                    return of(void 0);
                })
            );
    }

    register(data: RegisterRequest): Observable<void> {
        return this.http
            .post<void>(`${environment.apiUrl}/auth/register`, data)
            .pipe(
                tap({
                    next: () => {
                        this.router.navigate(['/login']);
                    },
                }),
                catchError((error) => {
                    console.error(
                        'AuthService: Registration failed for user:',
                        data.username,
                        'Error:',
                        error
                    );
                    return throwError(() => error);
                })
            );
    }

    private setSession(token: string): void {
        try {
            const decoded: any = jwtDecode(token);
            this.accessTokenSignal.set(token);
            this.tokenExpirySignal.set(decoded.exp);

            console.log('AuthService: User logged in, token set.');
            console.log('Token expires at (unix epoch):', decoded.exp);
            console.log('Token decoded:', decoded);
            console.log('Full token:', token);
        } catch (error) {
            this.doLogout();
        }
    }

    doLogout(): void {
        this.clearAuth();
        this.router.navigate(['/login']);
    }

    clearAuth(): void {
        this.accessTokenSignal.set(null);
        this.tokenExpirySignal.set(null);
        localStorage.removeItem('accessToken');
    }
}
