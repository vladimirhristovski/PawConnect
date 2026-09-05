import { Pipe, PipeTransform } from '@angular/core';
import { Business } from '../../core/models/business';

@Pipe({ name: 'primaryPhotoUrl' })
export class PrimaryPhotoUrlPipe implements PipeTransform {
  transform(business: Business): string | null {
    const primary = business.photos.find((p) => p.isPrimary);
    if (primary) return primary.url;
    return business.photos[0]?.url ?? null;
  }
}
