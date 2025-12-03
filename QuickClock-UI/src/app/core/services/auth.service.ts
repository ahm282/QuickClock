import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { catchError, Observable, of, tap, throwError, map } from 'rxjs';
import { jwtDecode, JwtPayload } from 'jwt-decode';

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

interface QuickClockJwtPayload extends JwtPayload {
    exp: number;
    sub: string;
    roles?: string[];
}

@Injectable({
    providedIn: 'root',
})
export class AuthService {
    private channel?: BroadcastChannel;
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

    constructor() {
        if (typeof BroadcastChannel !== 'undefined') {
            this.channel = new BroadcastChannel('quickclock-auth');
            this.channel.onmessage = this.handleChannelMessage.bind(this);
        }
    }

    // ---- Public API ----

    login(credentials: LoginRequest): Observable<AccessTokenResponse> {
        return this.http
            .post<AccessTokenResponse>(
                `${environment.apiUrl}/auth/login`,
                credentials,
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

    /**
     * Called at app bootstrap via provideAppInitializer.
     */
    initSession(): Observable<void> {
        if (this.isAccessTokenValid()) {
            return of(void 0);
        }

        return this.refresh().pipe(
            map(() => void 0),
            catchError(() => {
                this.clearAuth();
                return of(void 0);
            })
        );
    }

    // ---- Private helpers ----

    private handleChannelMessage(event: MessageEvent): void {
        const data = event.data;
        if (!data || typeof data !== 'object') {
            return;
        }

        if (data.type === 'token' && data.token && data.exp) {
            this.accessTokenSignal.set(data.token);
            this.tokenExpirySignal.set(data.exp);
        }

        if (data.type === 'logout') {
            this.clearAuth();
            this.router.navigate(['/login']);
        }
    }

    private setSession(token: string): void {
        try {
            const decoded: QuickClockJwtPayload =
                jwtDecode<QuickClockJwtPayload>(token);

            if (!decoded.exp) {
                throw new Error('Invalid or malformed token!');
            }

            this.accessTokenSignal.set(token);
            this.tokenExpirySignal.set(decoded.exp);

            this.channel?.postMessage({
                type: 'token',
                token,
                exp: decoded.exp,
            });

            if (!environment.production) {
                console.debug('Token decoded:', decoded);
                console.debug('Full token:', token);
                console.debug('AuthService: token exp:', decoded.exp);
            }
        } catch (error) {
            this.doLogout();
        }
    }

    doLogout(): void {
        this.clearAuth();

        if (this.channel) {
            // Broadcast to other tabs as well
            this.channel?.postMessage({ type: 'logout' });
        } else {
            // Fallback if BroadcastChannel is not supported
            this.router.navigate(['/login']);
        }
    }

    clearAuth(): void {
        this.accessTokenSignal.set(null);
        this.tokenExpirySignal.set(null);
    }
}
