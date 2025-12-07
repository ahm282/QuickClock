import { CommonModule } from '@angular/common';
import { Component, ElementRef, signal, ViewChild } from '@angular/core';
import {
    LucideAngularModule,
    Clock,
    QrCode,
    CircleAlert,
    RefreshCcw,
} from 'lucide-angular';

@Component({
    selector: 'app-attendance-scanner-component',
    imports: [CommonModule, LucideAngularModule],
    templateUrl: './attendance-scanner-component.component.html',
    styleUrl: './attendance-scanner-component.component.css',
})
export class AttendanceScannerComponentComponent {
    readonly Clock = Clock;
    readonly QrCode = QrCode;
    readonly CircleAlert = CircleAlert;
    readonly RefreshCcw = RefreshCcw;

    isClockedIn = signal<boolean>(false);
    scannerActive = signal<boolean>(false);
    cameraError = signal<string | null>(null);
    lastScanTime = signal<string | null>(null);

    availableCameras = signal<MediaDeviceInfo[]>([]);
    selectedDeviceId = signal<string | null>(null);
    loadingDevices = signal<boolean>(false);

    // Refs
    @ViewChild('videoElement') videoElement?: ElementRef<HTMLVideoElement>;
    private currentStream: MediaStream | null = null;

    ngOnInit(): void {
        const saved = localStorage.getItem('attendance.selectedDeviceId');
        if (saved) {
            this.selectedDeviceId.set(saved);
        }

        this.refreshCameras(true).catch(() => {});
    }

    ngOnDestroy(): void {
        this.stopCamera();
    }

    async refreshCameras(warmPermission = false): Promise<void> {
        if (
            !navigator.mediaDevices?.enumerateDevices ||
            !navigator.mediaDevices?.getUserMedia
        ) {
            this.cameraError.set('This browser does not support camera APIs.');
            return;
        }

        this.loadingDevices.set(true);
        this.cameraError.set(null);

        try {
            if (warmPermission) {
                const warm = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: false,
                });
                warm.getTracks().forEach((t) => t.stop());
            }

            const devices = (
                await navigator.mediaDevices.enumerateDevices()
            ).filter((d) => d.kind === 'videoinput');

            console.log(devices);

            if (!this.selectedDeviceId()) {
                const rearish =
                    devices.find((d) =>
                        /back|rear|environment|wide/i.test(d.label)
                    ) ?? devices[devices.length - 1]; // Many phones list rear last

                if (rearish) {
                    this.selectedDeviceId.set(rearish.deviceId);
                }
            }

            this.availableCameras.set(devices);

            if (
                this.selectedDeviceId() &&
                !devices.some((d) => d.deviceId === this.selectedDeviceId())
            ) {
                this.selectedDeviceId.set(devices[0]?.deviceId ?? null);
            }
        } catch (err) {
            console.error('enumerateDevices error:', err);
            this.cameraError.set('Could not list cameras. Check permissions.');
        } finally {
            this.loadingDevices.set(false);
        }
    }

    async onSelectCamera(deviceId: string): Promise<void> {
        this.selectedDeviceId.set(deviceId || null);
        localStorage.setItem('attendance.selectedDeviceId', deviceId || '');

        if (this.scannerActive()) {
            await this.restartCamera();
        }
    }

    toggleScanner(): void {
        if (this.scannerActive()) {
            this.stopCamera();
        } else {
            this.startCamera().then(() => {
                this.scannerActive.set(true);
            });
        }
    }

    private async restartCamera(): Promise<void> {
        this.stopCamera();
        await this.startCamera();
        this.scannerActive.set(true);
    }

    async startCamera(): Promise<void> {
        this.cameraError.set(null);
        this.scannerActive.set(true);

        try {
            const deviceId = this.selectedDeviceId();
            const constraints: MediaStreamConstraints = {
                video: deviceId
                    ? { deviceId: { exact: deviceId } }
                    : { facingMode: { ideal: 'environment' } },
                audio: false,
            };

            const stream = await navigator.mediaDevices.getUserMedia(
                constraints
            );
            this.currentStream = stream;

            setTimeout(() => {
                if (this.videoElement?.nativeElement) {
                    this.videoElement.nativeElement.srcObject = stream;
                }
            }, 50);
        } catch (err: any) {
            console.error('Error accessing camera:', err);
            const msg =
                err?.name === 'NotAllowedError'
                    ? 'Camera permission denied. Enable it in browser settings.'
                    : err?.name === 'NotFoundError'
                    ? 'Selected camera not found. Try another device in the list.'
                    : 'Could not access camera. Please check permissions and HTTPS.';
            this.cameraError.set(msg);
            this.scannerActive.set(false);
        }
    }

    stopCamera(): void {
        this.scannerActive.set(false);

        if (this.currentStream) {
            this.currentStream.getTracks().forEach((track) => track.stop());
            this.currentStream = null;
        }
    }

    handleScanResult(result: string): void {
        this.stopCamera();
        this.lastScanTime.set(new Date().toLocaleString());
        this.isClockedIn.update((v) => !v);
    }
}
