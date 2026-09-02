import { Component, inject, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { apiErrorMessage } from '../../../core/api-error';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-admin-users',
  imports: [FormField, RouterLink, Pagination],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers {
  protected adminService = inject(AdminService);

  filterModel = signal({ active: '', role: '' });
  filterForm = form(this.filterModel);

  error = signal<string | null>(null);

  constructor() {
    this.search();
  }

  search(): void {
    this.load(0);
  }

  goToPage(page: number): void {
    this.load(page);
  }

  toggleActive(id: number, currentlyActive: boolean): void {
    this.error.set(null);
    this.adminService.updateUserStatus(id, { active: !currentlyActive }).subscribe({
      next: () => this.search(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not update user status.')),
    });
  }

  remove(id: number, username: string): void {
    if (!confirm(`Permanently delete user "${username}"?`)) return;
    this.error.set(null);
    this.adminService.deleteUser(id).subscribe({
      next: () => this.search(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not delete user.')),
    });
  }

  private load(page: number): void {
    const { active, role } = this.filterModel();
    this.adminService.searchUsers({
      active: active === '' ? undefined : active === 'true',
      role: role || undefined,
      page,
      size: 20,
    });
  }
}
