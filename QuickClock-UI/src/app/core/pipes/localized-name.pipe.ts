import { Pipe, PipeTransform, Inject, LOCALE_ID } from '@angular/core';
import { UserSummaryDTO } from '../models/dto/user-summary-dto.model';
import { User } from '../models/user.model';

@Pipe({
    name: 'localizedName',
    standalone: true,
})
export class LocalizedNamePipe implements PipeTransform {
    constructor(@Inject(LOCALE_ID) private locale: string) {}

    transform(user: User | UserSummaryDTO | null | undefined): string {
        if (!user) {
            return '';
        }

        if (this.locale.startsWith('ar') && user.displayNameArabic) {
            return user.displayNameArabic.split(' ')[0]; // Return first name in Arabic
        }
        return user.displayName.split(' ')[0];
    }
}
