import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
    selector: 'app-kiosk-layout',
    standalone: true,
    imports: [RouterOutlet],
    templateUrl: './kiosk-layout.component.html',
    styleUrls: ['./kiosk-layout.component.css'],
})
export class KioskLayoutComponent {}
