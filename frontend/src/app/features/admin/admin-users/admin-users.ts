import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ReplaySubject, EMPTY, combineLatest, map, tap, switchMap, catchError } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { Pagination } from '../../../shared/pagination/pagination';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-admin-users',
  imports: [FormField, RouterLink, RouterLinkActive, Pagination],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  filterModel = signal({ active: '', role: '' });
  filterForm = form(this.filterModel);

  loading = signal(true);
  loadError = signal<string | null>(null);
  error = signal<string | null>(null);
  reload$ = new ReplaySubject<void>(1);

  page = toSignal(
    combineLatest([this.reload$, this.route.queryParamMap]).pipe(
      map(([, params]) => ({
        active: params.get('active') ?? '',
        role: params.get('role') ?? '',
        pageNum: Number(params.get('page') ?? 0),
      })),
      tap(({ active, role }) => {
        this.filterModel.set({ active, role });
        this.loading.set(true);
        this.loadError.set(null);
      }),
      switchMap(({ active, role, pageNum }) =>
        this.adminService
          .searchUsers({
            active: active === '' ? undefined : active === 'true',
            role: role || undefined,
            page: pageNum,
            size: 20,
          })
          .pipe(
            tap(() => this.loading.set(false)),
            catchError((err) => {
              this.loadError.set(apiErrorMessage(err, 'Could not load users.'));
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
    const { active, role } = this.filterModel();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { active: active || null, role: role || null, page: null },
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

  toggleActive(id: number, currentlyActive: boolean): void {
    this.error.set(null);
    this.adminService.updateUserStatus(id, { active: !currentlyActive }).subscribe({
      next: () => this.reload$.next(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not update user status.')),
    });
  }

  remove(id: number, username: string): void {
    if (!confirm(`Permanently delete user "${username}"?`)) return;
    this.error.set(null);
    this.adminService.deleteUser(id).subscribe({
      next: () => this.reload$.next(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not delete user.')),
    });
  }
}
