import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { BusinessService } from '../../../core/services/business';
import { LookupService } from '../../../core/services/lookup';
import { AuthService } from '../../../core/services/auth';
import { Pagination } from '../../../shared/pagination/pagination';
import { BusinessSearchParams } from '../../../core/models/business.model';
import { Coordinates } from '../../../core/models/coordinates.model';
import {
  getCurrentPosition,
  haversineDistanceKm,
} from '../../../shared/geo/geo-utils';

const FILTER_DEBOUNCE_MS = 400;

@Component({
  selector: 'app-business-search',
  imports: [FormsModule, RouterLink, Pagination],
  templateUrl: './business-search.html',
  styleUrl: './business-search.css',
})
export class BusinessSearch implements OnInit {
  protected businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  protected auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  filters: BusinessSearchParams = { page: 0, size: 12 };
  useNearby = signal(false);
  locating = signal(false);
  locationError = signal<string | null>(null);
  userCoords = signal<Coordinates | null>(null);

  private filterChange$ = new Subject<void>();

  constructor() {
    this.filterChange$
      .pipe(debounceTime(FILTER_DEBOUNCE_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.runSearch());
  }

  ngOnInit(): void {
    this.lookup.loadBusinessTypes();
    this.lookup.loadMunicipalities();
    this.search();
  }

  search(): void {
    this.filters.page = 0;
    this.runSearch();
  }

  onFilterChange(): void {
    this.filters.page = 0;
    this.filterChange$.next();
  }

  clearFilters(): void {
    this.filters = { page: 0, size: 12 };
    this.useNearby.set(false);
    this.userCoords.set(null);
    this.locationError.set(null);
    this.search();
  }

  goToPage(page: number): void {
    this.filters.page = page;
    this.runSearch();
  }

  toggleNearby(): void {
    this.useNearby.update((v) => !v);
    this.locationError.set(null);
    if (this.useNearby()) {
      this.filters.radiusKm = this.filters.radiusKm ?? 25;
      this.locateThenSearch();
    } else {
      this.filters.lat = undefined;
      this.filters.lng = undefined;
      this.userCoords.set(null);
      this.search();
    }
  }

  distanceLabel(biz: { latitude: number | null; longitude: number | null }): string | null {
    const coords = this.userCoords();
    if (!coords || biz.latitude == null || biz.longitude == null) return null;
    const km = haversineDistanceKm(coords, { lat: biz.latitude, lng: biz.longitude });
    return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
  }

  private locateThenSearch(): void {
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
        this.userCoords.set(coords);
        this.filters.lat = coords.lat;
        this.filters.lng = coords.lng;
        this.search();
      })
      .catch((err: Error) => {
        this.locating.set(false);
        this.locationError.set(err.message);
        this.useNearby.set(false);
      });
  }

  private runSearch(): void {
    this.businessService.search(this.filters);
  }
}
