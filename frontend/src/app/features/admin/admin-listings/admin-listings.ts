import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReplaySubject, EMPTY, combineLatest, map, tap, switchMap, catchError } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { ListingStatusCode } from '../../../core/models/listing';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-admin-listings',
  imports: [FormField, RouterLink, Pagination, DatePipe],
  templateUrl: './admin-listings.html',
  styleUrl: './admin-listings.css',
})
export class AdminListings {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  filterModel = signal({ status: '' });
  filterForm = form(this.filterModel);

  statuses: ListingStatusCode[] = ['DRAFT', 'ACTIVE', 'ADOPTED', 'EXPIRED', 'CANCELLED'];

  loading = signal(true);
  loadError = signal<string | null>(null);
  reload$ = new ReplaySubject<void>(1);

  page = toSignal(
    combineLatest([this.reload$, this.route.queryParamMap]).pipe(
      map(([, params]) => ({
        status: params.get('status') ?? '',
        pageNum: Number(params.get('page') ?? 0),
      })),
      tap(({ status }) => {
        this.filterModel.set({ status });
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap(({ status, pageNum }) =>
        this.adminService.searchListings(status || undefined, pageNum).pipe(
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

  onFilterChange(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status: this.filterModel().status || null, page: null },
      queryParamsHandling: 'merge',
    });
  }

  goToPage(pageNum: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: pageNum || null },
      queryParamsHandling: 'merge',
    });
  }
}
