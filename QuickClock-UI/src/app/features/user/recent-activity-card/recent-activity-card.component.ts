import { DatePipe, NgClass } from '@angular/common';
import { Component, input } from '@angular/core';
import type { Activity } from '../../../core/models/activity.model';

@Component({
    selector: 'app-recent-activity-card',
    imports: [DatePipe, NgClass],
    templateUrl: './recent-activity-card.component.html',
    styleUrl: './recent-activity-card.component.css',
})
export class RecentActivityCardComponent {
    activities = input.required<Activity[]>();
    loading = input.required<boolean>();
    error = input<string | null>();

    trackByIdx = (index: number) => index;

    label(type: Activity['type']): string {
        switch (type) {
            case 'clock_in':
                return 'Clock in';
            case 'lunch_start':
                return 'Lunch start';
            case 'lunch_end':
                return 'Lunch end';
            default:
                return type;
        }
    }
}
