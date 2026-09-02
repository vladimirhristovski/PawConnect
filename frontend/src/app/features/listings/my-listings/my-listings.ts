import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ListingService } from '../../../core/services/listing.service';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-my-listings',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './my-listings.html',
})
export class MyListings {
  protected listingService = inject(ListingService);

  constructor() {
    this.listingService.loadMine(0);
  }

  goToPage(page: number): void {
    this.listingService.loadMine(page);
  }
}
