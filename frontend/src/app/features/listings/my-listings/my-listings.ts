import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReplaySubject, EMPTY, combineLatest, map, tap, switchMap, catchError } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { StatusChip } from '../../../shared/ui/status-chip/status-chip';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-my-listings',
  imports: [RouterLink, Pagination, DatePipe, StatusChip],
  templateUrl: './my-listings.html',
  styleUrl: './my-listings.css',
})
export class MyListings {
  private listingService = inject(ListingService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  loading = signal(true);
  loadError = signal<string | null>(null);
  reload$ = new ReplaySubject<void>(1);

  page = toSignal(
    combineLatest([this.reload$, this.route.queryParamMap]).pipe(
      map(([, params]) => Number(params.get('page') ?? 0)),
      tap(() => {
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap((pageNum) =>
        this.listingService.getMine(pageNum).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loadError.set(apiErrorMessage(err, 'Could not load listings.'));
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
}
