import { Component, inject, input, effect, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BusinessService } from '../../../core/services/business.service';
import { AuthService } from '../../../core/services/auth.service';
import { Business } from '../../../core/models/business';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-business-detail',
  imports: [RouterLink],
  templateUrl: './business-detail.html',
  styleUrl: './business-detail.css',
})
export class BusinessDetail {
  private businessService = inject(BusinessService);
  protected auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();

  business = signal<Business | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  constructor() {
    effect(() => this.load(Number(this.id())));
  }

  isOwner = computed(() => {
    const biz = this.business();
    const user = this.auth.currentUser();
    return !!biz && !!user && biz.ownerUsername === user.username;
  });

  remove(): void {
    const biz = this.business();
    if (!biz) return;
    if (!confirm('Delete this business permanently?')) return;
    this.businessService.delete(biz.id).subscribe(() => this.router.navigate(['/businesses']));
  }

  private load(id: number): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.businessService.getById(id).subscribe({
      next: (biz) => {
        this.business.set(biz);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(apiErrorMessage(err, 'Could not load business.'));
        this.loading.set(false);
      },
    });
  }
}
