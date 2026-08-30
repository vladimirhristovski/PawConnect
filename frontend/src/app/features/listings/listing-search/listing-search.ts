import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { ListingService } from '../../../core/services/listing';
import { LookupService } from '../../../core/services/lookup';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingSearchParams } from '../../../core/models/listing.model';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';

const FILTER_DEBOUNCE_MS = 400;

@Component({
  selector: 'app-listing-search',
  imports: [FormsModule, RouterLink, Pagination],
  templateUrl: './listing-search.html',
  styleUrl: './listing-search.css',
})
export class ListingSearch implements OnInit {
  protected listingService = inject(ListingService);
  protected lookup = inject(LookupService);
  private destroyRef = inject(DestroyRef);

  filters: ListingSearchParams = { page: 0, size: 12 };
  useNearby = signal(false);
  locating = signal(false);
  locationError = signal<string | null>(null);

  private filterChange$ = new Subject<void>();

  constructor() {
    this.filterChange$
      .pipe(debounceTime(FILTER_DEBOUNCE_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.runSearch());
  }

  ngOnInit(): void {
    this.lookup.loadSpecies();
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
  }
}
