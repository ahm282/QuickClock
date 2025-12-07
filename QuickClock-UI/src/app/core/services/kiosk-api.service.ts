import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserSummaryDTO {
    publicId: string; // UUID
    displayName: string;
}

export interface ClockQRCodeResponseDTO {
    token: string;
    endpoint: string;
}

@Injectable({ providedIn: 'root' })
export class KioskApiService {
    private http = inject(HttpClient);

    listEmployees(): Observable<UserSummaryDTO[]> {
        return this.http.get<UserSummaryDTO[]>(
            `${environment.apiUrl}/kiosk/employees`
        );
    }

    generateInQRCode(publicId: string): Observable<ClockQRCodeResponseDTO> {
        return this.http.get<ClockQRCodeResponseDTO>(
            `${environment.apiUrl}/clock/qr/generate/in/${publicId}`
        );
    }

    generateOutQRCode(publicId: string): Observable<ClockQRCodeResponseDTO> {
        return this.http.get<ClockQRCodeResponseDTO>(
            `${environment.apiUrl}/clock/qr/generate/out/${publicId}`
        );
    }
}
