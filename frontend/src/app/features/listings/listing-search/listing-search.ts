import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ListingService } from '../../../core/services/listing';
import { LookupService } from '../../../core/services/lookup';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingSearchParams } from '../../../core/models/listing.model';

@Component({
  selector: 'app-listing-search',
  imports: [FormsModule, RouterLink, Pagination],
  templateUrl: './listing-search.html',
  styleUrl: './listing-search.css',
})
export class ListingSearch implements OnInit {
  protected listingService = inject(ListingService);
  protected lookup = inject(LookupService);

  filters: ListingSearchParams = { page: 0, size: 12 };
  useNearby = signal(false);
  locationError = signal<string | null>(null);

  ngOnInit(): void {
    this.lookup.loadSpecies();
    this.lookup.loadMunicipalities();
    this.search();
  }

  search(): void {
    this.filters.page = 0;
    this.runSearch();
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
    if (!navigator.geolocation) {
      this.locationError.set('Geolocation is not supported by this browser.');
      this.useNearby.set(false);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.filters.lat = pos.coords.latitude;
        this.filters.lng = pos.coords.longitude;
        this.search();
      },
      () => {
        this.locationError.set('Could not get your location.');
        this.useNearby.set(false);
      },
    );
  }

  private runSearch(): void {
    this.listingService.search(this.filters);
  }
}
