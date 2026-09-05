import { Component, inject, input, signal, computed } from '@angular/core';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { email, form, FormField, FormRoot, maxLength } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import {
  ReplaySubject,
  EMPTY,
  combineLatest,
  map,
  tap,
  switchMap,
  catchError,
  firstValueFrom,
} from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { CreateApplicationRequest } from '../../../core/models/application';
import { apiErrorMessage } from '../../../core/api-error';
import { StatusChip } from '../../../shared/ui/status-chip/status-chip';

@Component({
  selector: 'app-listing-detail',
  imports: [FormField, FormRoot, RouterLink, TitleCasePipe, StatusChip],
  templateUrl: './listing-detail.html',
  styleUrl: './listing-detail.css',
})
export class ListingDetail {
  private listingService = inject(ListingService);
  private applicationService = inject(ApplicationService);
  protected auth = inject(AuthService);
  private router = inject(Router);

  id = input.required<string>();

  loading = signal(true);
  loadError = signal<string | null>(null);
  reload$ = new ReplaySubject<void>(1);

  applicationModel = signal<ApplicationForm>({ message: '', contactPhone: '', contactEmail: '' });
  applicationSent = signal(false);
  applicationError = signal<string | null>(null);
  actionError = signal<string | null>(null);
  actionBusy = signal(false);

  listing = toSignal(
    combineLatest([this.reload$, toObservable(this.id)]).pipe(
      map(([, id]) => Number(id)),
      tap(() => {
        this.loading.set(true);
        this.loadError.set(null);
        this.applicationSent.set(false);
      }),
      switchMap((id) =>
        this.listingService.getById(id).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loadError.set(apiErrorMessage(err, 'Could not load listing.'));
            this.loading.set(false);
            return EMPTY;
          }),
        ),
      ),
    ),
  );

  applicationForm = form(
    this.applicationModel,
    (path) => {
      maxLength(path.message, 2000, { message: 'Message must be at most 2000 characters' });
      maxLength(path.contactPhone, 30, { message: 'Phone must be at most 30 characters' });
      email(path.contactEmail, { message: 'Enter a valid email' });
      maxLength(path.contactEmail, 255, { message: 'Email must be at most 255 characters' });
    },
    {
      submission: {
        action: async (form) => {
          const listing = this.listing();
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
            this.applicationError.set(apiErrorMessage(err, 'Could not submit application.'));
          }
          return;
        },
      },
    },
  );

  constructor() {
    this.reload$.next();
  }

  isOwner = computed(() => {
    const listing = this.listing();
    const user = this.auth.currentUser();
    return !!listing && !!user && listing.postedBy === user.username;
  });

  breedNames = computed(
    () =>
      this.listing()
        ?.pet.breeds.map((b) => b.name)
        .join(', ') ?? '',
  );

  publish(): void {
    const listing = this.listing();
    if (!listing) return;
    this.actionError.set(null);
    this.actionBusy.set(true);
    this.listingService.publish(listing.id).subscribe({
      next: () => {
        this.actionBusy.set(false);
        this.reload$.next();
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.actionError.set(apiErrorMessage(err, 'Could not publish the listing.'));
      },
    });
  }

  cancel(): void {
    const listing = this.listing();
    if (!listing) return;
    this.actionError.set(null);
    this.actionBusy.set(true);
    this.listingService.cancel(listing.id).subscribe({
      next: () => {
        this.actionBusy.set(false);
        this.reload$.next();
      },
      error: (err) => {
        this.actionBusy.set(false);
        this.actionError.set(apiErrorMessage(err, 'Could not cancel the listing.'));
      },
    });
  }

  remove(): void {
    const listing = this.listing();
    if (!listing) return;
    if (!confirm('Delete this listing permanently?')) return;
    this.actionError.set(null);
    this.actionBusy.set(true);
    this.listingService.delete(listing.id).subscribe({
      next: () => this.router.navigate(['/listings/mine']),
      error: (err) => {
        this.actionBusy.set(false);
        this.actionError.set(apiErrorMessage(err, 'Could not delete the listing.'));
      },
    });
  }
}

interface ApplicationForm {
  message: string;
  contactPhone: string;
  contactEmail: string;
}
