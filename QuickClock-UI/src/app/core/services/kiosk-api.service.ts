import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import {
    fetchEventSource,
    EventSourceMessage,
} from '@microsoft/fetch-event-source';
import { QrScanStatusDTO } from '../models/qr-scan-status.model';

export interface UserSummaryDTO {
    publicId: string;
    displayName: string;
    lastClockType: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | null;
    lastClockTime: string | null;
}

export interface ClockQRCodeResponseDTO {
    token: string;
    path: string;
    tokenId: string;
}

class FatalError extends Error {}

@Injectable({ providedIn: 'root' })
export class KioskApiService {
    private http = inject(HttpClient);
    private authService = inject(AuthService);

    listEmployees(): Observable<UserSummaryDTO[]> {
        return this.http.get<UserSummaryDTO[]>(
            `${environment.apiUrl}/kiosk/employees`,
        );
    }

    generateInQRCode(publicId: string): Observable<ClockQRCodeResponseDTO> {
        return this.http.get<ClockQRCodeResponseDTO>(
            `${environment.apiUrl}/clock/qr/generate/in/${publicId}`,
        );
    }

    generateOutQRCode(publicId: string): Observable<ClockQRCodeResponseDTO> {
        return this.http.get<ClockQRCodeResponseDTO>(
            `${environment.apiUrl}/clock/qr/generate/out/${publicId}`,
        );
    }

    listenForQrScan(
        tokenId: string,
    ): Observable<QrScanStatusDTO | 'connected'> {
        const apiUrl = environment.apiUrl;

        return new Observable<QrScanStatusDTO | 'connected'>((subscriber) => {
            const accessToken = this.authService.accessToken();

            if (!accessToken) {
                subscriber.error(
                    new Error('No access token available for Kiosk'),
                );
                return;
            }

            const controller = new AbortController();

            fetchEventSource(`${apiUrl}/clock/qr/stream/${tokenId}`, {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                    Accept: 'text/event-stream',
                },
                signal: controller.signal,

                async onopen(response) {
                    if (response.ok) {
                        subscriber.next('connected');
                        return;
                    } else if (
                        response.status >= 400 &&
                        response.status < 500
                    ) {
                        throw new FatalError();
                    }
                },

                onmessage: (message: EventSourceMessage) => {
                    // 1. Handle heartbeat (prevents timeouts)
                    if (message.event === 'ping') {
                        return;
                    }

                    // 2. Handle connection established (Resolves Race Condition)
                    if (message.event === 'init') {
                        subscriber.next('connected');
                        return;
                    }

                    // 3. Handle actual scan
                    if (message.event === 'scanned') {
                        try {
                            const data = JSON.parse(
                                message.data,
                            ) as QrScanStatusDTO;
                            subscriber.next(data);
                            subscriber.complete();
                        } catch (error) {
                            subscriber.error(error);
                        } finally {
                            controller.abort();
                        }
                    }
                },

                onerror: (err) => {
                    // Rethrow to stop retries if it's a fatal 4xx error
                    if (err instanceof FatalError) {
                        subscriber.error(err);
                        controller.abort();
                        throw err; // Stop retrying
                    }
                    // Otherwise, the library will auto-retry.
                    // We can log it, but don't abort strictly unless you want no retries at all.
                    console.warn('SSE connection error, retrying...', err);
                },
            });

            return () => {
                controller.abort();
            };
        });
    }
}
