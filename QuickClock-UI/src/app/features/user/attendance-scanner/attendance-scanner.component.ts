import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    signal,
    ViewChild,
    inject,
} from '@angular/core';
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
import { ClockService } from '../../../core/services/clock.service';

@Component({
    selector: 'app-attendance-scanner',
    imports: [CommonModule, LucideAngularModule],
    templateUrl: './attendance-scanner.component.html',
    styleUrl: './attendance-scanner.component.css',
})
export class AttendanceScannerComponent {
    readonly Clock = Clock;
    readonly Camera = Camera;
    readonly QrCode = QrCode;
    readonly CircleAlert = CircleAlert;
    readonly RefreshCcw = RefreshCcw;
    readonly X = X;
    readonly History = History;

    clockService = inject(ClockService);

    scannerActive = signal<boolean>(false);
    cameraError = signal<string | null>(null);
    lastScanTime = signal<string | null>(null);
    processingRequest = signal<boolean>(false);
    requestError = signal<string | null>(null);

    availableCameras = signal<MediaDeviceInfo[]>([]);
    selectedDeviceId = signal<string | null>(null);
    loadingDevices = signal<boolean>(false);

    hm = new Intl.DateTimeFormat(undefined, {
        hour: '2-digit',
        minute: '2-digit',
    });

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
                        },
                    );
            } catch (err) {
                console.error('Error starting scanner:', err);
                this.cameraError.set(
                    'Could not access camera. Please check permissions.',
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
        this.stopCamera();
        this.processingRequest.set(true);
        this.requestError.set(null);

        try {
            // Parse the QR code JSON directly (not base64 encoded)
            const qrData = JSON.parse(result) as {
                token: string;
                path: string;
            };

            // Send the clock in/out request through the service
            this.clockService.clockAction(qrData.path, qrData.token).subscribe({
                next: () => {
                    this.lastScanTime.set(this.hm.format(new Date()));
                    this.processingRequest.set(false);
                },
                error: (error) => {
                    console.error('Clock in/out failed:', error);
                    this.requestError.set(
                        error.error?.message ||
                            'Failed to process attendance. Please try again.',
                    );
                    this.processingRequest.set(false);
                },
            });
        } catch (error) {
            console.error('Failed to parse QR code:', error);
            this.requestError.set('Invalid QR code. Please try again.');
            this.processingRequest.set(false);
        }
    }
}
