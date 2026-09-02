import { Component, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, skip } from 'rxjs/operators';
import { ListingService } from '../../../core/services/listing.service';
import { LookupService } from '../../../core/services/lookup.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingSearchParams } from '../../../core/models/listing';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import {
  ParamSchema,
  filtersToQueryParams,
  readFiltersFromParams,
} from '../../../shared/query-params/query-param-sync';

const FILTER_DEBOUNCE_MS = 400;

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

const EMPTY_FILTER_FORM: ListingFilterForm = {
  speciesCode: '',
  municipalityCode: '',
  petSize: '',
  gender: '',
  goodWithKids: false,
  goodWithOtherPets: false,
  minFee: null,
  maxFee: null,
  radiusKm: null,
};

@Component({
  selector: 'app-listing-search',
  imports: [FormField, RouterLink, Pagination],
  templateUrl: './listing-search.html',
  styleUrl: './listing-search.css',
})
export class ListingSearch {
  protected listingService = inject(ListingService);
  protected lookup = inject(LookupService);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  filters: ListingSearchParams = { ...DEFAULT_FILTERS };
  filterModel = signal<ListingFilterForm>({ ...EMPTY_FILTER_FORM });
  filterForm = form(this.filterModel);

  useNearby = signal(false);
  locating = signal(false);
  locationError = signal<string | null>(null);

  private filterChange$ = new Subject<void>();
  private suppressNextParamSync = false;

  constructor() {
    this.filterChange$
      .pipe(debounceTime(FILTER_DEBOUNCE_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.runSearch());

    this.lookup.loadSpecies();
    this.lookup.loadMunicipalities();

    this.filters = readFiltersFromParams(this.route.snapshot.queryParamMap, FILTER_SCHEMA, {
      ...DEFAULT_FILTERS,
    });
    this.useNearby.set(this.filters.lat != null && this.filters.lng != null);
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
        this.useNearby.set(this.filters.lat != null && this.filters.lng != null);
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
      this.search();
    }
  }

  private locateThenSearch(): void {
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
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
    this.listingService.search(this.filters);
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
    this.filters.speciesCode = f.speciesCode || undefined;
    this.filters.municipalityCode = f.municipalityCode || undefined;
    this.filters.petSize = f.petSize || undefined;
    this.filters.gender = f.gender || undefined;
    this.filters.goodWithKids = f.goodWithKids || undefined;
    this.filters.goodWithOtherPets = f.goodWithOtherPets || undefined;
    this.filters.minFee = f.minFee ?? undefined;
    this.filters.maxFee = f.maxFee ?? undefined;
    this.filters.radiusKm = this.useNearby() ? (f.radiusKm ?? undefined) : undefined;
  }

  private syncFiltersToModel(): void {
    this.filterModel.set({
      speciesCode: this.filters.speciesCode ?? '',
      municipalityCode: this.filters.municipalityCode ?? '',
      petSize: this.filters.petSize ?? '',
      gender: this.filters.gender ?? '',
      goodWithKids: this.filters.goodWithKids ?? false,
      goodWithOtherPets: this.filters.goodWithOtherPets ?? false,
      minFee: this.filters.minFee ?? null,
      maxFee: this.filters.maxFee ?? null,
      radiusKm: this.filters.radiusKm ?? null,
    });
  }
}

interface ListingFilterForm {
  speciesCode: string;
  municipalityCode: string;
  petSize: string;
  gender: string;
  goodWithKids: boolean;
  goodWithOtherPets: boolean;
  minFee: number | null;
  maxFee: number | null;
  radiusKm: number | null;
}
