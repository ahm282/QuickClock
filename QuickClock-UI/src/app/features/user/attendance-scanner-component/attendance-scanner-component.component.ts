import { CommonModule } from '@angular/common';
import { Component, ElementRef, signal, ViewChild } from '@angular/core';
import {
    LucideAngularModule,
    Clock,
    QrCode,
    CircleAlert,
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

    isClockedIn = signal<boolean>(false);
    scannerActive = signal<boolean>(false);
    cameraError = signal<String | null>(null);
    lastScanTime = signal<string | null>(null);

    // Refs
    @ViewChild('videoElement') videoElement?: ElementRef<HTMLVideoElement>;
    private currentStream: MediaStream | null = null;

    ngOnDestroy(): void {
        this.stopCamera();
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

    async startCamera(): Promise<void> {
        this.cameraError.set(null);
        this.scannerActive.set(true);

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: 'environment' },
            });

            this.currentStream = stream;

            setTimeout(() => {
                if (this.videoElement?.nativeElement) {
                    this.videoElement.nativeElement.srcObject = stream;
                }
            }, 50);
        } catch (err) {
            console.error('Error accessing camera:', err);
            this.cameraError.set(
                'Could not access camera. Please check permissions.'
            );
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
