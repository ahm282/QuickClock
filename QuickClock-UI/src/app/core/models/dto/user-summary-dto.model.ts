export interface UserSummaryDTO {
    publicId: string;
    displayName: string;
    displayNameArabic: string;
    lastClockType: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | null;
    lastClockTime: string | null;
}
