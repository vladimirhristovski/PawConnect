import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingStatusCode } from '../../../core/models/listing.model';

@Component({
  selector: 'app-admin-listings',
  imports: [FormsModule, RouterLink, Pagination, DatePipe],
  templateUrl: './admin-listings.html',
  styleUrl: './admin-listings.css',
})
export class AdminListings implements OnInit {
  protected adminService = inject(AdminService);

  statusFilter = '';
  statuses: ListingStatusCode[] = ['DRAFT', 'ACTIVE', 'ADOPTED', 'EXPIRED', 'CANCELLED'];

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.adminService.searchListings(this.statusFilter || undefined, 0);
  }

  goToPage(page: number): void {
    this.adminService.searchListings(this.statusFilter || undefined, page);
  }
}
