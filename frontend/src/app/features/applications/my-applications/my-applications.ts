import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-my-applications',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './my-applications.html',
})
export class MyApplications {
  protected applicationService = inject(ApplicationService);

  constructor() {
    this.applicationService.loadMine(0);
  }

  goToPage(page: number): void {
    this.applicationService.loadMine(page);
  }

  withdraw(id: number): void {
    if (!confirm('Withdraw this application?')) return;
    this.applicationService.withdraw(id).subscribe(() => this.applicationService.loadMine(0));
  }

  canWithdraw(statusCode: string): boolean {
    return statusCode === 'SUBMITTED' || statusCode === 'UNDER_REVIEW';
  }
}
