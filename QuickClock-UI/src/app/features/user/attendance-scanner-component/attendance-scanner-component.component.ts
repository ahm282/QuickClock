import { CommonModule } from '@angular/common';
import { Component, ElementRef, signal, ViewChild } from '@angular/core';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { Result } from '@zxing/library';
import {
    LucideAngularModule,
    Clock,
    QrCode,
    CircleAlert,
    RefreshCcw,
    X,
    Camera,
    History,
} from 'lucide-angular';

@Component({
    selector: 'app-attendance-scanner-component',
    imports: [CommonModule, LucideAngularModule],
    templateUrl: './attendance-scanner-component.component.html',
    styleUrl: './attendance-scanner-component.component.css',
})
export class AttendanceScannerComponentComponent {
    readonly Clock = Clock;
    readonly Camera = Camera;
    readonly QrCode = QrCode;
    readonly CircleAlert = CircleAlert;
    readonly RefreshCcw = RefreshCcw;
    readonly X = X;
    readonly History = History;

    isClockedIn = signal<boolean>(false);
    scannerActive = signal<boolean>(false);
    cameraError = signal<string | null>(null);
    lastScanTime = signal<string | null>(null);

    availableCameras = signal<MediaDeviceInfo[]>([]);
    selectedDeviceId = signal<string | null>(null);
    loadingDevices = signal<boolean>(false);

    // Refs
    @ViewChild('videoElement') videoElement?: ElementRef<HTMLVideoElement>;

    private codeReader = new BrowserMultiFormatReader();
    private activeScanControls: IScannerControls | null = null;

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

        setTimeout(async () => {
            if (!this.videoElement?.nativeElement) return;

            try {
                this.activeScanControls =
                    await this.codeReader.decodeFromVideoDevice(
                        undefined, // undefined = use default/back camera
                        this.videoElement.nativeElement,
                        (result: Result | undefined, error: any) => {
                            if (result) {
                                this.handleScanResult(result.getText());
                            }
                        }
                    );
            } catch (err) {
                console.error('Error starting scanner:', err);
                this.cameraError.set(
                    'Could not access camera. Please check permissions.'
                );
                this.scannerActive.set(false);
            }
        }, 50);
    }

    stopCamera(): void {
        this.scannerActive.set(false);

        if (this.activeScanControls) {
            this.activeScanControls.stop();
            this.activeScanControls = null;
        }
    }

    handleScanResult(result: string): void {
        console.log('Result:', result);
        // Decode result and determine clock-in or clock-out
        var decoded = atob(result);
        console.log('Decoded:', decoded);

        this.stopCamera();
        this.lastScanTime.set(new Date().toLocaleTimeString());
        this.isClockedIn.update((v) => !v);
    }
}
