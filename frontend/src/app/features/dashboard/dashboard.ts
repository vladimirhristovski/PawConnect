import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListingService } from '../../core/services/listing.service';
import { AuthService } from '../../core/services/auth.service';
import { ListingSummary } from '../../core/models/listing';
import { Page } from '../../core/models/page';
import { apiErrorMessage } from '../../core/api-error';
import { ListingCard } from '../../shared/ui/listing-card/listing-card';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, ListingCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private listingService = inject(ListingService);
  protected auth = inject(AuthService);

  results = signal<Page<ListingSummary> | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  constructor() {
    this.listingService.search({ page: 0, size: 6 }).subscribe({
      next: (page) => {
        this.results.set(page);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(apiErrorMessage(err, 'Could not load listings.'));
        this.loading.set(false);
      },
    });
  }
}
