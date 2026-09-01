import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-account',
  imports: [],
  templateUrl: './account.html',
  styleUrl: './account.css',
})
export class Account {
  protected auth = inject(AuthService);

  confirming = signal(false);
  deleting = signal(false);
  error = signal<string | null>(null);

  startConfirm(): void {
    this.confirming.set(true);
    this.error.set(null);
  }

  cancelConfirm(): void {
    this.confirming.set(false);
  }

  deleteAccount(): void {
    this.deleting.set(true);
    this.error.set(null);
    this.auth.deleteAccount().subscribe({
      next: () => this.auth.clearSession(),
      error: (err) => {
        this.deleting.set(false);
        this.error.set(err.error?.message ?? 'Could not delete account.');
      },
    });
  }
}
