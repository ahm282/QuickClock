import { Pipe, PipeTransform, Inject, LOCALE_ID } from '@angular/core';
import { UserSummaryDTO } from '../models/dto/user-summary-dto.model';
import { User } from '../models/user.model';

@Pipe({
    name: 'localizedName',
    standalone: true,
})
export class LocalizedNamePipe implements PipeTransform {
    constructor(@Inject(LOCALE_ID) private locale: string) {}

    transform(
        user: User | UserSummaryDTO | null | undefined,
        format: 'first' | 'full' = 'first'
    ): string {
        if (!user) {
            return '';
        }

        const name = this.locale.startsWith('ar') && user.displayNameArabic
            ? user.displayNameArabic
            : user.displayName;

        return format === 'full' ? name : name.split(' ')[0];
    }
}
