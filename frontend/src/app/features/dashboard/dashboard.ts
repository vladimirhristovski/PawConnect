import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListingService } from '../../core/services/listing.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  protected listingService = inject(ListingService);
  protected auth = inject(AuthService);

  constructor() {
    this.listingService.search({ page: 0, size: 6 });
  }
}
