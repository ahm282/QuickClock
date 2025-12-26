import { Component, inject } from '@angular/core';
import { ClockService } from '../../../core/services/clock.service';
import { LucideAngularModule, ClipboardClock } from 'lucide-angular';

@Component({
    selector: 'app-work-hours-card',
    standalone: true,
    imports: [LucideAngularModule],
    templateUrl: './work-hours-card.component.html',
    styleUrl: './work-hours-card.component.css',
})
export class WorkHoursCardComponent {
    clockService = inject(ClockService);
    readonly ClipboardClock = ClipboardClock;
}
