export interface Activity {
    id: number;
    type: 'in' | 'break_start' | 'break_end' | 'out';
    recordedAt: string;
}
