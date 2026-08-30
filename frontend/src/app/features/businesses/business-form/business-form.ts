import { Component, inject, input, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BusinessService } from '../../../core/services/business';
import { LookupService } from '../../../core/services/lookup';
import { CreateBusinessRequest, UpdateBusinessRequest } from '../../../core/models/business.model';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';

@Component({
  selector: 'app-business-form',
  imports: [FormsModule],
  templateUrl: './business-form.html',
  styleUrl: './business-form.css',
})
export class BusinessForm {
  protected businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  private router = inject(Router);

  id = input<string>();

  model: CreateBusinessRequest = {
    typeCode: '',
    name: '',
    phone: '',
    address: '',
    municipalityCode: '',
  };
  countryCode?: string;
  cityCode?: string;

  submitting = signal(false);
  error = signal<string | null>(null);

  locating = signal(false);
  locationError = signal<string | null>(null);

  constructor() {
    this.lookup.loadBusinessTypes();
    this.lookup.loadCountries();

    effect(() => {
      const idParam = this.id();
      if (idParam) this.businessService.loadOne(Number(idParam));
    });

    effect(() => {
      const existing = this.businessService.selected();
      if (existing && this.isEdit()) {
        this.model = {
          typeCode: existing.typeCode,
          name: existing.name,
          description: existing.description ?? undefined,
          phone: existing.phone,
          email: existing.email ?? undefined,
          address: existing.address,
          municipalityCode: existing.municipalityCode,
          latitude: existing.latitude ?? undefined,
          longitude: existing.longitude ?? undefined,
        };
        this.lookup.loadMunicipalities();
      }
    });
  }

  isEdit(): boolean {
    return !!this.id();
  }

  onCountryChange(): void {
    this.cityCode = undefined;
    this.model.municipalityCode = '';
    this.lookup.loadCities(this.countryCode);
  }

  onCityChange(): void {
    this.model.municipalityCode = '';
    this.lookup.loadMunicipalities(this.cityCode);
  }

  useMyLocation(): void {
    this.locationError.set(null);
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
        this.model.latitude = coords.lat;
        this.model.longitude = coords.lng;
      })
      .catch((err: Error) => {
        this.locating.set(false);
        this.locationError.set(err.message);
      });
  }

  submit(): void {
    this.error.set(null);
    this.submitting.set(true);

    if (this.isEdit()) {
      const payload: UpdateBusinessRequest = { ...this.model };
      this.businessService.update(Number(this.id()), payload).subscribe({
        next: (updated) => this.router.navigate(['/businesses', updated.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.message ?? 'Could not save changes.');
        },
      });
    } else {
      this.businessService.create(this.model).subscribe({
        next: (created) => this.router.navigate(['/businesses', created.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.message ?? 'Could not create business.');
        },
      });
    }
  }
}
