import { Component } from '@angular/core';
import { LucideAngularModule, Clock } from 'lucide-angular';

@Component({
    selector: 'app-logo',
    standalone: true,
    imports: [LucideAngularModule],
    templateUrl: './app-logo.component.html',
    styleUrl: './app-logo.component.css',
})
export class AppLogoComponent {
    readonly clock = Clock;
}
