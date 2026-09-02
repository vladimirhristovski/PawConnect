import { Component, inject, input, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ApplicationService } from '../../../core/services/application.service';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-listing-applications',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './listing-applications.html',
})
export class ListingApplications {
  protected applicationService = inject(ApplicationService);

  id = input.required<string>();

  constructor() {
    effect(() => {
      this.applicationService.loadForListing(Number(this.id()), 0);
    });
  }

  goToPage(page: number): void {
    this.applicationService.loadForListing(Number(this.id()), page);
  }

  decide(appId: number, decision: 'APPROVE' | 'REJECT'): void {
    this.applicationService
      .review(appId, decision)
      .subscribe(() => this.applicationService.loadForListing(Number(this.id()), 0));
  }

  canDecide(statusCode: string): boolean {
    return statusCode === 'SUBMITTED' || statusCode === 'UNDER_REVIEW';
  }
}
