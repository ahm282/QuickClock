import { JwtPayload } from 'jwt-decode';

export interface QuickClockJwtPayload extends JwtPayload {
    username?: string;
    displayName?: string;
    displayNameArabic?: string;
    exp: number;
    sub: string;
    roles?: string[];
}
