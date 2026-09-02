import { Component, inject, input, effect, signal } from '@angular/core';
import { form, FormField, FormRoot } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { CreateApplicationRequest } from '../../../core/models/application';

@Component({
  selector: 'app-listing-detail',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './listing-detail.html',
  styleUrl: './listing-detail.css',
})
export class ListingDetail {
  protected listingService = inject(ListingService);
  protected applicationService = inject(ApplicationService);
  protected auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();

  applicationModel = signal<ApplicationForm>({ message: '', contactPhone: '', contactEmail: '' });
  applicationSent = signal(false);
  applicationError = signal<string | null>(null);

  applicationForm = form(this.applicationModel, () => {}, {
    submission: {
      action: async (form) => {
        const listing = this.listingService.selected();
        if (!listing) return;
        this.applicationError.set(null);
        const value = form().value();
        const payload: CreateApplicationRequest = {
          message: value.message || undefined,
          contactPhone: value.contactPhone || undefined,
          contactEmail: value.contactEmail || undefined,
        };
        try {
          await firstValueFrom(this.applicationService.submit(listing.id, payload));
          this.applicationSent.set(true);
        } catch (err) {
          const detail = (err as { error?: { detail?: string } }).error?.detail;
          this.applicationError.set(detail ?? 'Could not submit application.');
        }
        return;
      },
    },
  });

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

  publish(): void {
    const listing = this.listingService.selected();
    if (!listing) return;
    this.listingService.publish(listing.id).subscribe(() => this.listingService.loadOne(listing.id));
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
    this.listingService.delete(listing.id).subscribe(() => this.router.navigate(['/listings/mine']));
  }
}

interface ApplicationForm {
  message: string;
  contactPhone: string;
  contactEmail: string;
}
