import { Component, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, skip } from 'rxjs/operators';
import { BusinessService } from '../../../core/services/business.service';
import { LookupService } from '../../../core/services/lookup.service';
import { AuthService } from '../../../core/services/auth.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { BusinessSearchParams, Business } from '../../../core/models/business';
import { Page } from '../../../core/models/page';
import { Coordinates } from '../../../core/models/coordinates';
import { apiErrorMessage } from '../../../core/api-error';
import { getCurrentPosition, haversineDistanceKm } from '../../../shared/geo/geo-utils';
import {
  ParamSchema,
  filtersToQueryParams,
  readFiltersFromParams,
} from '../../../shared/query-params/query-param-sync';

const FILTER_DEBOUNCE_MS = 400;

const DEFAULT_FILTERS: BusinessSearchParams = { page: 0, size: 12 };

const FILTER_SCHEMA: ParamSchema<BusinessSearchParams> = {
  typeCode: 'string',
  municipalityCode: 'string',
  lat: 'number',
  lng: 'number',
  radiusKm: 'number',
  page: 'number',
  size: 'number',
};

const EMPTY_FILTER_FORM: BusinessFilterForm = {
  typeCode: '',
  municipalityCode: '',
  radiusKm: null,
};

@Component({
  selector: 'app-business-search',
  imports: [FormField, RouterLink, Pagination],
  templateUrl: './business-search.html',
  styleUrl: './business-search.css',
})
export class BusinessSearch {
  private businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  protected auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  filters: BusinessSearchParams = { ...DEFAULT_FILTERS };
  filterModel = signal<BusinessFilterForm>({ ...EMPTY_FILTER_FORM });
  filterForm = form(this.filterModel);

  results = signal<Page<Business> | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  useNearby = signal(false);
  locating = signal(false);
  locationError = signal<string | null>(null);
  userCoords = signal<Coordinates | null>(null);

  private filterChange$ = new Subject<void>();
  private suppressNextParamSync = false;

  constructor() {
    this.filterChange$
      .pipe(debounceTime(FILTER_DEBOUNCE_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.runSearch());

    this.lookup.loadBusinessTypes();
    this.lookup.loadMunicipalities();

    this.filters = readFiltersFromParams(this.route.snapshot.queryParamMap, FILTER_SCHEMA, {
      ...DEFAULT_FILTERS,
    });
    this.applyGeoFromFilters();
    this.syncFiltersToModel();
    this.runSearch();

    this.route.queryParamMap
      .pipe(skip(1), takeUntilDestroyed(this.destroyRef))
      .subscribe((paramMap) => {
        if (this.suppressNextParamSync) {
          this.suppressNextParamSync = false;
          return;
        }
        this.filters = readFiltersFromParams(paramMap, FILTER_SCHEMA, { ...DEFAULT_FILTERS });
        this.applyGeoFromFilters();
        this.syncFiltersToModel();
        this.runSearch();
      });
  }

  search(): void {
    this.syncModelToFilters();
    this.filters.page = 0;
    this.runSearch();
  }

  onFilterChange(): void {
    this.syncModelToFilters();
    this.filters.page = 0;
    this.filterChange$.next();
  }

  clearFilters(): void {
    this.filters = { ...DEFAULT_FILTERS };
    this.filterModel.set({ ...EMPTY_FILTER_FORM });
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
      const radiusKm = this.filterModel().radiusKm ?? 25;
      this.filterModel.update((m) => ({ ...m, radiusKm }));
      this.filters.radiusKm = radiusKm;
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

  primaryPhotoUrl(biz: Business): string | null {
    const primary = biz.photos.find((p) => p.isPrimary);
    if (primary) return primary.url;
    return biz.photos.length > 0 ? biz.photos[0].url : null;
  }

  private applyGeoFromFilters(): void {
    if (this.filters.lat != null && this.filters.lng != null) {
      this.useNearby.set(true);
      this.userCoords.set({ lat: this.filters.lat, lng: this.filters.lng });
    } else {
      this.useNearby.set(false);
      this.userCoords.set(null);
    }
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
    this.loading.set(true);
    this.loadError.set(null);
    this.businessService.search(this.filters).subscribe({
      next: (page) => {
        this.results.set(page);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(apiErrorMessage(err, 'Could not load businesses.'));
        this.loading.set(false);
      },
    });
    this.updateUrl();
  }

  private updateUrl(): void {
    const queryParams = filtersToQueryParams(this.filters, FILTER_SCHEMA, DEFAULT_FILTERS);
    this.suppressNextParamSync = true;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true,
    });
  }

  private syncModelToFilters(): void {
    const f = this.filterModel();
    this.filters.typeCode = f.typeCode || undefined;
    this.filters.municipalityCode = f.municipalityCode || undefined;
    this.filters.radiusKm = this.useNearby() ? (f.radiusKm ?? undefined) : undefined;
  }

  private syncFiltersToModel(): void {
    this.filterModel.set({
      typeCode: this.filters.typeCode ?? '',
      municipalityCode: this.filters.municipalityCode ?? '',
      radiusKm: this.filters.radiusKm ?? null,
    });
  }
}

interface BusinessFilterForm {
  typeCode: string;
  municipalityCode: string;
  radiusKm: number | null;
}
