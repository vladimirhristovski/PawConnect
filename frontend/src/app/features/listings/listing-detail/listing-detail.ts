import { Component, inject, input, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ListingService } from '../../../core/services/listing';
import { ApplicationService } from '../../../core/services/application';
import { AuthService } from '../../../core/services/auth';
import { CreateApplicationRequest } from '../../../core/models/application.model';

@Component({
  selector: 'app-listing-detail',
  imports: [FormsModule, RouterLink],
  templateUrl: './listing-detail.html',
  styleUrl: './listing-detail.css',
})
export class ListingDetail {
  protected listingService = inject(ListingService);
  protected applicationService = inject(ApplicationService);
  protected auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();

  applicationForm: CreateApplicationRequest = {};
  applicationSent = signal(false);
  applicationError = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.listingService.loadOne(Number(this.id()));
      this.applicationSent.set(false);
    });
  }

  isOwner(): boolean {
    const listing = this.listingService.selected();
    const user = this.auth.currentUser();
    return !!listing && !!user && listing.postedBy === user.username;
  }

  breedNames(listing: NonNullable<ReturnType<ListingService['selected']>>): string {
    return listing.pet.breeds.map((b) => b.name).join(', ');
  }

  submitApplication(): void {
    const listing = this.listingService.selected();
    if (!listing) return;
    this.applicationError.set(null);
    this.applicationService.submit(listing.id, this.applicationForm).subscribe({
      next: () => this.applicationSent.set(true),
      error: (err) =>
        this.applicationError.set(err.error?.detail ?? 'Could not submit application.'),
    });
  }

  publish(): void {
    const listing = this.listingService.selected();
    if (!listing) return;
    this.listingService
      .publish(listing.id)
      .subscribe(() => this.listingService.loadOne(listing.id));
  }

  cancel(): void {
    const listing = this.listingService.selected();
    if (!listing) return;
    this.listingService.cancel(listing.id).subscribe(() => this.listingService.loadOne(listing.id));
  }

  remove(): void {
    const listing = this.listingService.selected();
    if (!listing) return;
    if (!confirm('Delete this listing permanently?')) return;
    this.listingService
      .delete(listing.id)
      .subscribe(() => this.router.navigate(['/listings/mine']));
  }
}
