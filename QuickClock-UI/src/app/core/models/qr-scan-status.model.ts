export interface QrScanStatusDTO {
    tokenId: string;
    userPublicId: string;
    userDisplayName: string;
    direction: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END';
    clockedAt: string; // ISO date string from backend
}
