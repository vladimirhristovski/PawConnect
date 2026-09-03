import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ReplaySubject, EMPTY, combineLatest, map, tap, switchMap, catchError } from 'rxjs';
import { ApplicationService } from '../../../core/services/application.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-my-applications',
  imports: [RouterLink, Pagination, DatePipe],
  templateUrl: './my-applications.html',
})
export class MyApplications {
  private applicationService = inject(ApplicationService);
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
        this.applicationService.getMine(pageNum).pipe(
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

  withdraw(id: number): void {
    if (!confirm('Withdraw this application?')) return;
    this.applicationService.withdraw(id).subscribe(() => this.reload$.next());
  }

  canWithdraw(statusCode: string): boolean {
    return statusCode === 'SUBMITTED' || statusCode === 'UNDER_REVIEW';
  }
}
