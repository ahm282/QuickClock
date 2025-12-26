import { Injectable, inject, signal, LOCALE_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { tap, catchError, of } from 'rxjs';
import { WorkHoursDTO } from '../models/work-hours.model';
import { ClockStatusDTO } from '../models/clock-status.model';
import { Activity } from '../models/activity.model';
import { ClockActionRequest } from '../models/clock-action-request.model';

@Injectable({
    providedIn: 'root',
})
export class ClockService {
    private http = inject(HttpClient);
    private locale = inject(LOCALE_ID);

    // Time formatter
    private hm = new Intl.DateTimeFormat(this.locale, {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
    });

    // Reactive state using signals
    workHours = signal<WorkHoursDTO | null>(null);
    workHoursLoading = signal<boolean>(false);
    workHoursError = signal<string | null>(null);

    clockStatus = signal<ClockStatusDTO | null>(null);
    clockStatusLoading = signal<boolean>(false);
    clockStatusError = signal<string | null>(null);

    activities = signal<Activity[]>([]);
    activitiesLoading = signal<boolean>(false);
    activitiesError = signal<string | null>(null);

    /**
     * Fetch current clock status for the logged-in user
     */
    fetchClockStatus() {
        this.clockStatusLoading.set(true);
        this.clockStatusError.set(null);

        this.http
            .get<ClockStatusDTO>(`${environment.apiUrl}/clock/status/me`)
            .pipe(
                tap((status) => {
                    // Format the time if available
                    if (status.lastClockTime) {
                        const date = new Date(status.lastClockTime);
                        status.lastClockTime = this.hm.format(date);
                    }
                    this.clockStatus.set(status);
                    this.clockStatusLoading.set(false);
                }),
                catchError((error) => {
                    console.error('Failed to fetch clock status:', error);
                    this.clockStatusError.set('Failed to load clock status.');
                    this.clockStatusLoading.set(false);
                    return of(null);
                }),
            )
            .subscribe();
    }

    /**
     * Fetch work hours for the logged-in user
     */
    fetchWorkHours() {
        this.workHoursLoading.set(true);
        this.workHoursError.set(null);

        this.http
            .get<WorkHoursDTO>(`${environment.apiUrl}/clock/hours/me`)
            .pipe(
                tap((hours) => {
                    this.workHours.set(hours);
                    this.workHoursLoading.set(false);
                }),
                catchError((error) => {
                    console.error('Failed to fetch work hours:', error);
                    this.workHoursError.set('Failed to load work hours.');
                    this.workHoursLoading.set(false);
                    return of(null);
                }),
            )
            .subscribe();
    }

    /**
     * Fetch recent activities for the logged-in user
     */
    fetchActivities() {
        this.activitiesLoading.set(true);
        this.activitiesError.set(null);

        this.http
            .get<Activity[]>(`${environment.apiUrl}/clock/activity/me`)
            .pipe(
                tap((data) => {
                    this.activities.set(data);
                    this.activitiesLoading.set(false);
                }),
                catchError((error) => {
                    console.error('Failed to fetch activities:', error);
                    this.activitiesError.set(
                        'Failed to load recent activities.',
                    );
                    this.activitiesLoading.set(false);
                    return of(null);
                }),
            )
            .subscribe();
    }

    /**
     * Clock in or out using QR code token
     */
    clockAction(path: string, token: string) {
        const url = `${environment.apiUrl}${path}`;

        return this.http.post(url, { token }).pipe(
            tap(() => {
                // Refresh all related data after successful clock action
                this.fetchClockStatus();
                this.fetchWorkHours();
                this.fetchActivities();
            }),
        );
    }

    /**
     * Initialize all data
     */
    initializeData() {
        this.fetchClockStatus();
        this.fetchWorkHours();
        this.fetchActivities();
    }

    /**
     * Refresh all data
     */
    refreshAll() {
        this.fetchClockStatus();
        this.fetchWorkHours();
        this.fetchActivities();
    }
}
