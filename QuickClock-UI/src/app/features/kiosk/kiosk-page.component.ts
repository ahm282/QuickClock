import {
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    NgZone,
    signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { QRCodeComponent } from 'angularx-qrcode';
import {
    KioskApiService,
    UserSummaryDTO,
} from '../../core/services/kiosk-api.service';
import { LogoutButtonComponent } from '../../shared/components/logout-button/logout-button.component';
import {
    LucideAngularModule,
    ArrowLeft,
    QrCode,
    Clock,
    LoaderCircle,
} from 'lucide-angular';
import { AppLogoComponent } from '../../shared/components/app-logo/app-logo.component';
import { QrScanStatusDTO } from '../../core/models/qr-scan-status.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-kiosk-page',
    standalone: true,
    imports: [
        CommonModule,
        QRCodeComponent,
        LogoutButtonComponent,
        LucideAngularModule,
        AppLogoComponent,
    ],
    templateUrl: './kiosk-page.component.html',
    styleUrl: './kiosk-page.component.css',
})
export class KioskPageComponent {
    private api = inject(KioskApiService);
    private destroyRef = inject(DestroyRef);
    private ngZone = inject(NgZone);

    readonly clock = Clock;
    readonly ArrowLeft = ArrowLeft;
    readonly QrCode = QrCode;
    readonly LoaderCircle = LoaderCircle;

    employees = signal<UserSummaryDTO[]>([]);
    loading = signal(true);
    error = signal<string | null>(null);

    selectedPublicId = signal<string | null>(null);
    selectedEmployee = computed(
        () =>
            this.employees().find(
                (e) => e.publicId === this.selectedPublicId()
            ) ?? null
    );

    timeRemaining = signal(30);
    scanSuccess = signal(false);
    scanStatus = signal<QrScanStatusDTO | null>(null);

    // QR State
    qrToken = signal<string | null>(null);
    qrPath = signal<string | null>(null);
    qrLoading = signal(false);
    sseConnected = signal(false);

    today = signal<Date>(new Date());
    private timerId: number | null = null;
    private scanSub: Subscription | null = null;

    private currentPurpose = signal<'clock-in' | 'clock-out'>('clock-in');

    qrData = computed(() => {
        const token = this.qrToken();
        const path = this.qrPath();
        if (!token || !path) return '';
        return JSON.stringify({ token, path });
    });

    // Only show QR when we have data AND the connection is established
    isQrVisible = computed(() => {
        return !!this.qrData() && this.sseConnected();
    });

    constructor() {
        this.api.listEmployees().subscribe({
            next: (users) => {
                this.employees.set(users);
                this.loading.set(false);
            },
            error: () => {
                this.error.set('Failed to load employees.');
                this.loading.set(false);
            },
        });

        this.destroyRef.onDestroy(() => {
            this.stopTimer();
            this.scanSub?.unsubscribe();
        });

        // Start/stop countdown based on selection + success state
        effect(() => {
            const emp = this.selectedEmployee();
            const status = this.scanStatus();

            this.stopTimer();

            if (!emp || status) {
                this.timeRemaining.set(30);
                return;
            }

            this.timeRemaining.set(30);

            // Optimization: Run timer outside Angular zone
            this.ngZone.runOutsideAngular(() => {
                this.timerId = window.setInterval(() => {
                    // Check value without triggering CD yet
                    const current = this.timeRemaining();
                    if (current <= 1) {
                        // Back to zone for updates
                        this.ngZone.run(() => {
                            const purpose = this.currentPurpose();
                            this.refreshQrForSelected(purpose);
                            this.timeRemaining.set(30);
                        });
                    } else {
                        this.ngZone.run(() => {
                            this.timeRemaining.set(current - 1);
                        });
                    }
                }, 1000);
            });
        });
    }

    select(u: UserSummaryDTO) {
        this.selectedPublicId.set(u.publicId);

        const latestEmployee = this.employees().find(
            (e) => e.publicId === u.publicId
        );
        if (!latestEmployee) return;

        const nextPurpose =
            latestEmployee.lastClockType === 'IN' ? 'clock-out' : 'clock-in';

        this.currentPurpose.set(nextPurpose);
        this.resetQrState();
        this.error.set(null);
        this.refreshQrForSelected(nextPurpose);
    }

    back() {
        this.selectedPublicId.set(null);
        this.scanSuccess.set(false);
        this.scanStatus.set(null);
        this.timeRemaining.set(30);
        this.resetQrState();
        this.scanSub?.unsubscribe();
    }

    generateIn() {
        this.currentPurpose.set('clock-in');
        this.refreshQrForSelected('clock-in');
    }

    generateOut() {
        this.currentPurpose.set('clock-out');
        this.refreshQrForSelected('clock-out');
    }

    private refreshQrForSelected(purpose: 'clock-in' | 'clock-out') {
        const publicId = this.selectedPublicId();
        if (!publicId) return;

        this.qrLoading.set(true);
        this.sseConnected.set(false); // Reset connection state
        this.scanSuccess.set(false);
        this.scanStatus.set(null);

        const call =
            purpose === 'clock-in'
                ? this.api.generateInQRCode(publicId)
                : this.api.generateOutQRCode(publicId);

        call.subscribe({
            next: (res) => {
                this.qrToken.set(res.token);
                this.qrPath.set(res.path);
                this.qrLoading.set(false);

                if (res.tokenId) {
                    this.startScanListener(res.tokenId);
                }
            },
            error: () => {
                this.error.set('Failed to generate QR.');
                this.qrLoading.set(false);
            },
        });
    }

    private startScanListener(tokenId: string) {
        this.scanSub?.unsubscribe();
        this.scanStatus.set(null);
        this.sseConnected.set(false);

        this.scanSub = this.api.listenForQrScan(tokenId).subscribe({
            next: (event) => {
                if (event === 'connected') {
                    this.sseConnected.set(true);
                } else {
                    this.scanStatus.set(event);
                    this.scanSuccess.set(true);
                    this.stopTimer();

                    this.updateEmployeeStatus(event);
                    this.refreshEmployeeList();

                    window.setTimeout(() => {
                        this.scanSuccess.set(false);
                        this.scanStatus.set(null);
                        this.back();
                    }, 5000);
                }
            },
            error: (error) => {
                this.error.set('Connection lost. Please try again.');
                this.sseConnected.set(false);
                this.resetQrState();
            },
        });
    }

    private updateEmployeeStatus(scanStatus: QrScanStatusDTO): void {
        const currentEmployees = this.employees();
        const updatedEmployees = currentEmployees.map((emp) => {
            if (emp.publicId === scanStatus.userPublicId) {
                return {
                    ...emp,
                    lastClockType: scanStatus.direction, // 'IN' or 'OUT'
                    lastClockTime: scanStatus.clockedAt, // ISO date string
                };
            }
            return emp;
        });
        this.employees.set(updatedEmployees);
    }

    private refreshEmployeeList(): void {
        this.api.listEmployees().subscribe({
            next: (users) => {
                this.employees.set(users);
            },
            error: (err) => {
                console.error('Failed to refresh employee list:', err);
            },
        });
    }

    private resetQrState() {
        this.qrToken.set(null);
        this.qrPath.set(null);
        this.qrLoading.set(false);
        this.sseConnected.set(false);
    }

    private stopTimer() {
        if (this.timerId != null) {
            clearInterval(this.timerId);
            this.timerId = null;
        }
    }
}
