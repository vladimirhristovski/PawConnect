import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin';
import { Pagination } from '../../../shared/pagination/pagination';

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule, RouterLink, Pagination],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers implements OnInit {
  protected adminService = inject(AdminService);

  activeFilter = '';
  roleFilter = '';
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.adminService.searchUsers({
      active: this.activeFilter === '' ? undefined : this.activeFilter === 'true',
      role: this.roleFilter || undefined,
      page: 0,
      size: 20,
    });
  }

  goToPage(page: number): void {
    this.adminService.searchUsers({
      active: this.activeFilter === '' ? undefined : this.activeFilter === 'true',
      role: this.roleFilter || undefined,
      page,
      size: 20,
    });
  }

  toggleActive(id: number, currentlyActive: boolean): void {
    this.error.set(null);
    this.adminService.updateUserStatus(id, { active: !currentlyActive }).subscribe({
      next: () => this.search(),
      error: (err) => this.error.set(err.error?.detail ?? 'Could not update user status.'),
    });
  }

  remove(id: number, username: string): void {
    if (!confirm(`Permanently delete user "${username}"?`)) return;
    this.error.set(null);
    this.adminService.deleteUser(id).subscribe({
      next: () => this.search(),
      error: (err) => this.error.set(err.error?.detail ?? 'Could not delete user.'),
    });
  }
}
