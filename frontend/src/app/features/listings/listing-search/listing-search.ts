import { Component, inject, signal, computed, linkedSignal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, map, tap, switchMap, catchError } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { LookupService } from '../../../core/services/lookup.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingCard } from '../../../shared/ui/listing-card/listing-card';
import { MapPicker } from '../../../shared/map-picker/map-picker';
import { ListingSearchParams, ListingSummary } from '../../../core/models/listing';
import { Gender, Size } from '../../../core/models/pet';
import { Coordinates } from '../../../core/models/coordinates';
import { Page } from '../../../core/models/page';
import { apiErrorMessage } from '../../../core/api-error';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import {
  ParamSchema,
  filtersToQueryParams,
  readFiltersFromParams,
} from '../../../shared/query-params/query-param-sync';

const DEFAULT_FILTERS: ListingSearchParams = { page: 0, size: 12 };

const FILTER_SCHEMA: ParamSchema<ListingSearchParams> = {
  speciesCode: 'string',
  municipalityCode: 'string',
  petSize: 'string',
  gender: 'string',
  goodWithKids: 'boolean',
  goodWithOtherPets: 'boolean',
  minFee: 'number',
  maxFee: 'number',
  lat: 'number',
  lng: 'number',
  radiusKm: 'number',
  page: 'number',
  size: 'number',
};

function toFilterForm(filters: ListingSearchParams): ListingFilterForm {
  return {
    speciesCode: filters.speciesCode ?? '',
    municipalityCode: filters.municipalityCode ?? '',
    petSize: filters.petSize ?? '',
    gender: filters.gender ?? '',
    goodWithKids: filters.goodWithKids ?? false,
    goodWithOtherPets: filters.goodWithOtherPets ?? false,
    minFee: filters.minFee ?? null,
    maxFee: filters.maxFee ?? null,
    radiusKm: filters.radiusKm ?? null,
  };
}

@Component({
  selector: 'app-listing-search',
  imports: [FormField, DecimalPipe, Pagination, ListingCard, MapPicker],
  templateUrl: './listing-search.html',
  styleUrl: './listing-search.css',
})
export class ListingSearch {
  private listingService = inject(ListingService);
  protected lookup = inject(LookupService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private paramsSignal = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  currentFilters = computed(() =>
    readFiltersFromParams(this.paramsSignal(), FILTER_SCHEMA, DEFAULT_FILTERS),
  );

  filterModel = linkedSignal<ListingFilterForm>(() => toFilterForm(this.currentFilters()));
  filterForm = form(this.filterModel);

  loading = signal(true);
  loadError = signal<string | null>(null);

  results = toSignal<Page<ListingSummary> | null>(
    this.route.queryParamMap.pipe(
      map((pm) => readFiltersFromParams(pm, FILTER_SCHEMA, DEFAULT_FILTERS)),
      tap(() => {
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap((filters) =>
        this.listingService.search(filters).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loadError.set(apiErrorMessage(err, 'Could not load listings.'));
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
  showLocationPicker = signal(false);

  constructor() {
    this.lookup.loadSpecies();
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
    this.showLocationPicker.set(false);
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
        this.locationError.set(`${err.message} You can pick a spot on the map instead.`);
      });
  }

  openLocationPicker(): void {
    this.showLocationPicker.set(true);
  }

  onLocationPicked(coords: Coordinates): void {
    this.showLocationPicker.set(false);
    this.locationError.set(null);
    this.applyNearbyCoords(coords);
  }

  onLocationPickerCancelled(): void {
    this.showLocationPicker.set(false);
  }

  private applyNearbyCoords(coords: Coordinates): void {
    const radiusKm = this.filterModel().radiusKm ?? 25;
    this.navigateWithFilters(
      { ...this.currentFilters(), lat: coords.lat, lng: coords.lng, radiusKm, page: 0 },
      true,
    );
  }

  private buildFiltersFromForm(): ListingSearchParams {
    const f = this.filterModel();
    const current = this.currentFilters();
    return {
      speciesCode: f.speciesCode || undefined,
      municipalityCode: f.municipalityCode || undefined,
      petSize: f.petSize || undefined,
      gender: f.gender || undefined,
      goodWithKids: f.goodWithKids || undefined,
      goodWithOtherPets: f.goodWithOtherPets || undefined,
      minFee: f.minFee ?? undefined,
      maxFee: f.maxFee ?? undefined,
      lat: current.lat,
      lng: current.lng,
      radiusKm: this.useNearby() ? (f.radiusKm ?? undefined) : undefined,
      page: 0,
    };
  }

  private navigateWithFilters(filters: ListingSearchParams, replaceUrl: boolean): void {
    const queryParams = filtersToQueryParams(filters, FILTER_SCHEMA, DEFAULT_FILTERS);
    this.router.navigate([], { relativeTo: this.route, queryParams, replaceUrl });
  }
}

interface ListingFilterForm {
  speciesCode: string;
  municipalityCode: string;
  petSize: Size | '';
  gender: Gender | '';
  goodWithKids: boolean;
  goodWithOtherPets: boolean;
  minFee: number | null;
  maxFee: number | null;
  radiusKm: number | null;
}
