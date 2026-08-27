import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ListingService } from '../../../core/services/listing';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-my-listings',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './my-listings.html',
})
export class MyListings implements OnInit {
  protected listingService = inject(ListingService);

  ngOnInit(): void {
    this.listingService.loadMine(0);
  }
  goToPage(page: number): void {
    this.listingService.loadMine(page);
  }
}
