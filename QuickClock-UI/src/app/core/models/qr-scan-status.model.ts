export interface QrScanStatusDTO {
    tokenId: string;
    userDisplayName: string;
    direction: 'IN' | 'OUT';
    clockedAt: string; // ISO date string from backend
}
