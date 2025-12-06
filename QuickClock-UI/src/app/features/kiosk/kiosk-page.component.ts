import {
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { QRCodeComponent } from 'angularx-qrcode';
import {
    KioskApiService,
    UserSummaryDTO,
} from '../../core/services/kiosk-api.service';

@Component({
    selector: 'app-kiosk-page',
    standalone: true,
    imports: [CommonModule, QRCodeComponent],
    templateUrl: './kiosk-page.component.html',
    styleUrl: './kiosk-page.component.css',
})
export class KioskPageComponent {
    private api = inject(KioskApiService);
    private destroyRef = inject(DestroyRef);

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

    qrToken = signal<string | null>(null);
    qrEndpoint = signal<string | null>(null);
    qrLoading = signal(false);

    today = signal<Date>(new Date());

    private timerId: number | null = null;

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

        // Start/stop countdown based on selection + success state
        effect(() => {
            const emp = this.selectedEmployee();
            const success = this.scanSuccess();

            this.stopTimer();

            if (!emp || success) return;

            // reset
            this.timeRemaining.set(30);

            this.timerId = window.setInterval(() => {
                const v = this.timeRemaining();
                if (v <= 1) {
                    // refresh QR
                    this.refreshQrForSelected('clock-in');
                    this.timeRemaining.set(30);
                } else {
                    this.timeRemaining.set(v - 1);
                }
            }, 1000);

            this.destroyRef.onDestroy(() => this.stopTimer());
        });
    }

    select(u: UserSummaryDTO) {
        this.selectedPublicId.set(u.publicId);
        this.qrToken.set(null);
        this.qrEndpoint.set(null);
        this.error.set(null);

        // default to clock-in QR on selection (optional UX)
        this.refreshQrForSelected('clock-in');
    }

    back() {
        this.selectedPublicId.set(null);
        this.scanSuccess.set(false);
        this.timeRemaining.set(30);
        this.qrToken.set(null);
        this.qrEndpoint.set(null);
    }

    generateIn() {
        this.refreshQrForSelected('clock-in');
    }

    generateOut() {
        this.refreshQrForSelected('clock-out');
    }

    private refreshQrForSelected(purpose: 'clock-in' | 'clock-out') {
        const id = this.selectedPublicId();
        if (!id) return;

        this.qrLoading.set(true);
        const call =
            purpose === 'clock-in'
                ? this.api.generateInQRCode(id)
                : this.api.generateOutQRCode(id);

        call.subscribe({
            next: (res) => {
                this.qrToken.set(res.token);
                this.qrEndpoint.set(res.endpoint);
                this.qrLoading.set(false);
            },
            error: () => {
                this.error.set('Failed to generate QR.');
                this.qrLoading.set(false);
            },
        });
    }

    // Optional: if you want kiosk-only demo success
    simulateScanSuccess() {
        if (!this.selectedEmployee()) return;

        this.scanSuccess.set(true);
        window.setTimeout(() => {
            this.scanSuccess.set(false);
            this.back();
        }, 2500);
    }

    private stopTimer() {
        if (this.timerId != null) {
            clearInterval(this.timerId);
            this.timerId = null;
        }
    }
}
