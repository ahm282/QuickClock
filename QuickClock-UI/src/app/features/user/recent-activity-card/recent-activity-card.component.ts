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
        switch (type.toLocaleLowerCase()) {
            case 'in':
                return $localize`:@@clockInLabel:Clock in`;
            case 'break_start':
                return $localize`:@@breakStartLabel:Break start`;
            case 'break_end':
                return $localize`:@@breakEndLabel:Break end`;
            case 'out':
                return $localize`:@@clockOutLabel:Clock out`;
            default:
                return type;
        }
    }

    isEntry(type: Activity['type']): boolean {
        return ['in', 'break_end'].includes(type.toLowerCase());
    }
}
