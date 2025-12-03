import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { Observable, tap, catchError, of } from 'rxjs';

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

    private accessTokenSignal = signal<string | null>(null);

    public accessToken = this.accessTokenSignal.asReadonly();
    public isAuthenticated = computed(() => !!this.accessTokenSignal());

    constructor() {
        this.initializeAuth();
    }

    private initializeAuth(): void {
        this.refresh().subscribe({
            next: () => {
                if (this.isAuthenticated()) {
                    this.router.navigate(['/dashboard']);
                }
            },
            error: () => {
                // Silent failure
            },
        });
    }

    login(credentials: LoginRequest): Observable<AccessTokenResponse> {
        return this.http
            .post<AccessTokenResponse>(
                `${environment.apiUrl}/auth/login`,
                credentials
            )
            .pipe(
                tap((response) => {
                    this.accessTokenSignal.set(response.accessToken);
                })
            );
    }

    refresh(): Observable<AccessTokenResponse> {
        return this.http
            .post<AccessTokenResponse>(`${environment.apiUrl}/auth/refresh`, {})
            .pipe(
                tap((response) => {
                    this.accessTokenSignal.set(response.accessToken);
                }),
                catchError(() => {
                    this.clearAuth();
                    return of({ accessToken: '' });
                })
            );
    }

    logout(): Observable<any> {
        return this.http.post(`${environment.apiUrl}/auth/logout`, {}).pipe(
            tap(() => {
                this.clearAuth();
                this.router.navigate(['/login']);
            }),
            catchError(() => {
                this.clearAuth();
                this.router.navigate(['/login']);
                return of(null);
            })
        );
    }

    register(data: RegisterRequest): Observable<void> {
        return this.http.post<void>(
            `${environment.apiUrl}/auth/register`,
            data
        );
    }

    clearAuth(): void {
        this.accessTokenSignal.set(null);
    }
}
