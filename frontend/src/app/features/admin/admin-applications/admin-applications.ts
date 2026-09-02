import { Component, inject, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { ApplicationStatusCode } from '../../../core/models/application';

@Component({
  selector: 'app-admin-applications',
  imports: [FormField, RouterLink, Pagination, DatePipe],
  templateUrl: './admin-applications.html',
  styleUrl: './admin-applications.css',
})
export class AdminApplications {
  protected adminService = inject(AdminService);

  filterModel = signal({ status: '' });
  filterForm = form(this.filterModel);

  statuses: ApplicationStatusCode[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'WITHDRAWN',
    'CLOSED',
  ];

  constructor() {
    this.search();
  }

  search(): void {
    this.adminService.searchApplications(this.filterModel().status || undefined, 0);
  }

  goToPage(page: number): void {
    this.adminService.searchApplications(this.filterModel().status || undefined, page);
  }
}
