import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListingService } from '../../core/services/listing';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  protected listingService = inject(ListingService);
  protected auth = inject(AuthService);

  ngOnInit(): void {
    this.listingService.search({ page: 0, size: 6 });
  }
}
