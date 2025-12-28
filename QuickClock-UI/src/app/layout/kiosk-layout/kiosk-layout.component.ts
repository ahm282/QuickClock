import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-kiosk-layout',
    standalone: true,
    imports: [RouterOutlet, CommonModule],
    templateUrl: './kiosk-layout.component.html',
    styleUrls: ['./kiosk-layout.component.css'],
})
export class KioskLayoutComponent implements OnInit, OnDestroy {
    currentYear: number = new Date().getFullYear();
    currentTime: Date = new Date();
    private timeInterval: any;

    ngOnInit() {
        this.timeInterval = setInterval(() => {
            this.currentTime = new Date();
        }, 1000);
    }

    ngOnDestroy() {
        if (this.timeInterval) {
            clearInterval(this.timeInterval);
        }
    }
}
