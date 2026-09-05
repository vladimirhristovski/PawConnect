import { Pipe, PipeTransform } from '@angular/core';
import { Coordinates } from '../../core/models/coordinates';
import { haversineDistanceKm } from '../geo/geo-utils';

@Pipe({ name: 'distanceLabel' })
export class DistanceLabelPipe implements PipeTransform {
  transform(
    target: { latitude: number | null; longitude: number | null },
    from: Coordinates | null,
  ): string | null {
    if (!from || target.latitude == null || target.longitude == null) return null;
    const km = haversineDistanceKm(from, { lat: target.latitude, lng: target.longitude });
    return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
  }
}
