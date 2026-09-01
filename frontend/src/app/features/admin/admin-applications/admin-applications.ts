import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AdminService } from '../../../core/services/admin';
import { Pagination } from '../../../shared/pagination/pagination';
import { ApplicationStatusCode } from '../../../core/models/application.model';

@Component({
  selector: 'app-admin-applications',
  imports: [FormsModule, RouterLink, Pagination, DatePipe],
  templateUrl: './admin-applications.html',
  styleUrl: './admin-applications.css',
})
export class AdminApplications implements OnInit {
  protected adminService = inject(AdminService);

  statusFilter = '';
  statuses: ApplicationStatusCode[] = [
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'WITHDRAWN',
    'CLOSED',
  ];

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.adminService.searchApplications(this.statusFilter || undefined, 0);
  }

  goToPage(page: number): void {
    this.adminService.searchApplications(this.statusFilter || undefined, page);
  }
}
