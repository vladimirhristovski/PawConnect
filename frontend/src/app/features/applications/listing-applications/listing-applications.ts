import { Component, inject, input, signal } from '@angular/core';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReplaySubject, EMPTY, combineLatest, map, tap, switchMap, catchError } from 'rxjs';
import { ApplicationService } from '../../../core/services/application.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-listing-applications',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './listing-applications.html',
})
export class ListingApplications {
  private applicationService = inject(ApplicationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id = input.required<string>();

  loading = signal(true);
  loadError = signal<string | null>(null);
  actionError = signal<string | null>(null);
  actionBusy = signal(false);
  reload$ = new ReplaySubject<void>(1);

  page = toSignal(
    combineLatest([this.reload$, toObservable(this.id), this.route.queryParamMap]).pipe(
      map(([, id, params]) => ({
        listingId: Number(id),
        pageNum: Number(params.get('page') ?? 0),
      })),
      tap(() => {
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap(({ listingId, pageNum }) =>
        this.applicationService.getForListing(listingId, pageNum).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loadError.set(apiErrorMessage(err, 'Could not load applications.'));
            this.loading.set(false);
            return EMPTY;
          }),
        ),
      ),
    ),
  );

  constructor() {
    this.reload$.next();
  }

  goToPage(pageNum: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: pageNum || null },
      queryParamsHandling: 'merge',
    });
  }

  decide(appId: number, decision: 'APPROVE' | 'REJECT'): void {
    this.actionError.set(null);
    this.actionBusy.set(true);
    this.applicationService.review(appId, decision).subscribe({
      next: () => {
        this.actionBusy.set(false);
        this.reload$.next();
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.actionError.set(apiErrorMessage(err, 'Could not update the application.'));
      },
    });
  }

  canDecide(statusCode: string): boolean {
    return statusCode === 'SUBMITTED' || statusCode === 'UNDER_REVIEW';
  }
}
