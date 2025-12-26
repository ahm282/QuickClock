import { Component, inject } from '@angular/core';
import { IntlNumberPipe } from '../../../core/pipes/intl-number.pipe';
import { ClockService } from '../../../core/services/clock.service';

@Component({
    selector: 'app-work-hours-card',
    standalone: true,
    imports: [],
    templateUrl: './work-hours-card.component.html',
    styleUrl: './work-hours-card.component.css',
})
export class WorkHoursCardComponent {
    clockService = inject(ClockService);
}
