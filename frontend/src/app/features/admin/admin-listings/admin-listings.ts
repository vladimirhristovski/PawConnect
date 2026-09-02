import { Component, inject, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingStatusCode } from '../../../core/models/listing';

@Component({
  selector: 'app-admin-listings',
  imports: [FormField, RouterLink, Pagination, DatePipe],
  templateUrl: './admin-listings.html',
  styleUrl: './admin-listings.css',
})
export class AdminListings {
  protected adminService = inject(AdminService);

  filterModel = signal({ status: '' });
  filterForm = form(this.filterModel);

  statuses: ListingStatusCode[] = ['DRAFT', 'ACTIVE', 'ADOPTED', 'EXPIRED', 'CANCELLED'];

  constructor() {
    this.search();
  }

  search(): void {
    this.adminService.searchListings(this.filterModel().status || undefined, 0);
  }

  goToPage(page: number): void {
    this.adminService.searchListings(this.filterModel().status || undefined, page);
  }
}
