import { Pipe, PipeTransform, Inject, LOCALE_ID } from '@angular/core';
import { UserSummaryDTO } from '../models/dto/user-summary-dto.model';

@Pipe({
    name: 'localizedName',
    standalone: true,
})
export class LocalizedNamePipe implements PipeTransform {
    constructor(@Inject(LOCALE_ID) private locale: string) {}

    transform(user: UserSummaryDTO | null | undefined): string {
        if (!user) {
            return '';
        }

        if (this.locale.startsWith('ar') && user.displayNameArabic) {
            console.log('Using Arabic name:', user.displayNameArabic);
            return user.displayNameArabic;
        }
        return user.displayName;
    }
}
