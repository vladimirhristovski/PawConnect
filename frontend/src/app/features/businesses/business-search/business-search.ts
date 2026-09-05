import { Component, inject, signal, computed, linkedSignal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, map, tap, switchMap, catchError } from 'rxjs';
import { BusinessService } from '../../../core/services/business.service';
import { LookupService } from '../../../core/services/lookup.service';
import { AuthService } from '../../../core/services/auth.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { BusinessSearchParams, Business } from '../../../core/models/business';
import { Page } from '../../../core/models/page';
import { Coordinates } from '../../../core/models/coordinates';
import { apiErrorMessage } from '../../../core/api-error';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import {
  ParamSchema,
  filtersToQueryParams,
  readFiltersFromParams,
} from '../../../shared/query-params/query-param-sync';
import { PrimaryPhotoUrlPipe } from '../../../shared/pipes/primary-photo-url.pipe';
import { DistanceLabelPipe } from '../../../shared/pipes/distance-label.pipe';

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

function toFilterForm(filters: BusinessSearchParams): BusinessFilterForm {
  return {
    typeCode: filters.typeCode ?? '',
    municipalityCode: filters.municipalityCode ?? '',
    radiusKm: filters.radiusKm ?? null,
  };
}

@Component({
  selector: 'app-business-search',
  imports: [FormField, RouterLink, Pagination, PrimaryPhotoUrlPipe, DistanceLabelPipe],
  templateUrl: './business-search.html',
  styleUrl: './business-search.css',
})
export class BusinessSearch {
  private businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  protected auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private paramsSignal = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  currentFilters = computed(() =>
    readFiltersFromParams(this.paramsSignal(), FILTER_SCHEMA, DEFAULT_FILTERS),
  );

  filterModel = linkedSignal<BusinessFilterForm>(() => toFilterForm(this.currentFilters()));
  filterForm = form(this.filterModel);

  loading = signal(true);
  loadError = signal<string | null>(null);

  results = toSignal<Page<Business> | null>(
    this.route.queryParamMap.pipe(
      map((pm) => readFiltersFromParams(pm, FILTER_SCHEMA, DEFAULT_FILTERS)),
      tap(() => {
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap((filters) =>
        this.businessService.search(filters).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loadError.set(apiErrorMessage(err, 'Could not load businesses.'));
            this.loading.set(false);
            return EMPTY;
          }),
        ),
      ),
    ),
    { initialValue: null },
  );

  useNearby = computed(() => {
    const f = this.currentFilters();
    return f.lat != null && f.lng != null;
  });
  nearbyCoords = computed<Coordinates | null>(() => {
    const f = this.currentFilters();
    return f.lat != null && f.lng != null ? { lat: f.lat, lng: f.lng } : null;
  });

  locating = signal(false);
  locationError = signal<string | null>(null);

  constructor() {
    this.lookup.loadBusinessTypes();
    this.lookup.loadMunicipalities();
  }

  search(): void {
    this.navigateWithFilters(this.buildFiltersFromForm(), true);
  }

  onFilterChange(): void {
    this.search();
  }

  clearFilters(): void {
    this.locationError.set(null);
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  goToPage(page: number): void {
    this.navigateWithFilters({ ...this.currentFilters(), page }, false);
  }

  toggleNearby(): void {
    this.locationError.set(null);
    if (this.useNearby()) {
      const { lat, lng, radiusKm, ...rest } = this.currentFilters();
      this.navigateWithFilters({ ...rest, page: 0 }, true);
    } else {
      this.locateThenSearch();
    }
  }

  private locateThenSearch(): void {
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
        this.applyNearbyCoords(coords);
      })
      .catch((err: Error) => {
        this.locating.set(false);
        this.locationError.set(err.message);
      });
  }

  private applyNearbyCoords(coords: Coordinates): void {
    const radiusKm = this.filterModel().radiusKm ?? 25;
    this.navigateWithFilters(
      { ...this.currentFilters(), lat: coords.lat, lng: coords.lng, radiusKm, page: 0 },
      true,
    );
  }

  private buildFiltersFromForm(): BusinessSearchParams {
    const f = this.filterModel();
    const current = this.currentFilters();
    return {
      typeCode: f.typeCode || undefined,
      municipalityCode: f.municipalityCode || undefined,
      lat: current.lat,
      lng: current.lng,
      radiusKm: this.useNearby() ? (f.radiusKm ?? undefined) : undefined,
      page: 0,
    };
  }

  private navigateWithFilters(filters: BusinessSearchParams, replaceUrl: boolean): void {
    const queryParams = filtersToQueryParams(filters, FILTER_SCHEMA, DEFAULT_FILTERS);
    this.router.navigate([], { relativeTo: this.route, queryParams, replaceUrl });
  }
}

interface BusinessFilterForm {
  typeCode: string;
  municipalityCode: string;
  radiusKm: number | null;
}
