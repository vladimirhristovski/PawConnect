import { Component, inject, input, effect } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BusinessService } from '../../../core/services/business.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-business-detail',
  imports: [RouterLink],
  templateUrl: './business-detail.html',
  styleUrl: './business-detail.css',
})
export class BusinessDetail {
  protected businessService = inject(BusinessService);
  protected auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();

  constructor() {
    effect(() => {
      this.businessService.loadOne(Number(this.id()));
    });
  }

  isOwner(): boolean {
    const biz = this.businessService.selected();
    const user = this.auth.currentUser();
    return !!biz && !!user && biz.ownerUsername === user.username;
  }

  remove(): void {
    const biz = this.businessService.selected();
    if (!biz) return;
    if (!confirm('Delete this business permanently?')) return;
    this.businessService.delete(biz.id).subscribe(() => this.router.navigate(['/businesses']));
  }
}
