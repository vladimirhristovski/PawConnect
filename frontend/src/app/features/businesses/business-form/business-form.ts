import { Component, inject, input, effect, signal } from '@angular/core';
import { disabled, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { BusinessService } from '../../../core/services/business.service';
import { LookupService } from '../../../core/services/lookup.service';
import { PhotoService } from '../../../core/services/photo.service';
import {
  CreateBusinessRequest,
  UpdateBusinessRequest,
  StagedBusinessPhoto,
} from '../../../core/models/business';
import { Coordinates } from '../../../core/models/coordinates';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import { MapPicker } from '../../../shared/map-picker/map-picker';

@Component({
  selector: 'app-business-form',
  imports: [FormField, FormRoot, RouterLink, MapPicker],
  templateUrl: './business-form.html',
  styleUrl: './business-form.css',
})
export class BusinessForm {
  protected businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  private photoService = inject(PhotoService);
  private router = inject(Router);

  id = input<string>();

  formModel = signal<BusinessFormModel>({
    typeCode: '',
    name: '',
    description: '',
    phone: '',
    email: '',
    address: '',
    municipalityCode: '',
    latitude: null,
    longitude: null,
    countryCode: '',
    cityCode: '',
  });

  stagedPhotos = signal<StagedBusinessPhoto[]>([]);
  uploading = signal(false);
  uploadError = signal<string | null>(null);
  error = signal<string | null>(null);

  locating = signal(false);
  locationError = signal<string | null>(null);
  showMapPicker = signal(false);

  businessForm = form(
    this.formModel,
    (path) => {
      required(path.name, { message: 'Name is required' });
      required(path.typeCode, { message: 'Type is required' });
      required(path.phone, { message: 'Phone is required' });
      required(path.address, { message: 'Address is required' });
      required(path.municipalityCode, { message: 'Municipality is required' });
      disabled(path.cityCode, { when: ({ valueOf }) => !valueOf(path.countryCode) });
    },
    {
      submission: {
        action: async (form) => {
          this.error.set(null);
          const value = form().value();
          const base = {
            typeCode: value.typeCode,
            name: value.name,
            description: value.description || undefined,
            phone: value.phone,
            email: value.email || undefined,
            address: value.address,
            municipalityCode: value.municipalityCode,
            latitude: value.latitude ?? undefined,
            longitude: value.longitude ?? undefined,
          };
          try {
            if (this.isEdit()) {
              const payload: UpdateBusinessRequest = base;
              const updated = await firstValueFrom(
                this.businessService.update(Number(this.id()), payload),
              );
              this.router.navigate(['/businesses', updated.id]);
            } else {
              const payload: CreateBusinessRequest = {
                ...base,
                photos: this.stagedPhotos().map(({ previewName, ...rest }) => rest),
              };
              const created = await firstValueFrom(this.businessService.create(payload));
              this.router.navigate(['/businesses', created.id]);
            }
          } catch (err) {
            const detail = (err as { error?: { detail?: string } }).error?.detail;
            this.error.set(
              detail ?? (this.isEdit() ? 'Could not save changes.' : 'Could not create business.'),
            );
          }
          return;
        },
      },
    },
  );

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
        this.formModel.set({
          typeCode: existing.typeCode,
          name: existing.name,
          description: existing.description ?? '',
          phone: existing.phone,
          email: existing.email ?? '',
          address: existing.address,
          municipalityCode: existing.municipalityCode,
          latitude: existing.latitude ?? null,
          longitude: existing.longitude ?? null,
          countryCode: '',
          cityCode: '',
        });
        this.lookup.loadMunicipalities();
      }
    });
  }

  isEdit(): boolean {
    return !!this.id();
  }

  onCountryChange(): void {
    this.formModel.update((m) => ({ ...m, cityCode: '', municipalityCode: '' }));
    this.lookup.loadCities(this.formModel().countryCode || undefined);
  }

  onCityChange(): void {
    this.formModel.update((m) => ({ ...m, municipalityCode: '' }));
    this.lookup.loadMunicipalities(this.formModel().cityCode || undefined);
  }

  useMyLocation(): void {
    this.locationError.set(null);
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
        this.formModel.update((m) => ({ ...m, latitude: coords.lat, longitude: coords.lng }));
      })
      .catch((err: Error) => {
        this.locating.set(false);
        this.locationError.set(err.message);
      });
  }

  currentCoordinates(): Coordinates | null {
    const { latitude, longitude } = this.formModel();
    if (latitude == null || longitude == null) return null;
    return { lat: latitude, lng: longitude };
  }

  openMapPicker(): void {
    this.showMapPicker.set(true);
  }

  onMapConfirmed(coords: Coordinates): void {
    this.formModel.update((m) => ({ ...m, latitude: coords.lat, longitude: coords.lng }));
    this.showMapPicker.set(false);
  }

  onMapCancelled(): void {
    this.showMapPicker.set(false);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files || files.length === 0) return;

    this.uploadError.set(null);
    this.uploading.set(true);
    let remaining = files.length;
    Array.from(files).forEach((file) => {
      this.photoService.uploadTemp(file).subscribe({
        next: (res) => {
          const isFirst = this.stagedPhotos().length === 0;
          this.stagedPhotos.update((list) => [
            ...list,
            { url: res.url, isPrimary: isFirst, displayOrder: list.length, previewName: file.name },
          ]);
        },
        error: (err) => {
          this.uploadError.set(err.error?.detail ?? 'Upload failed.');
        },
        complete: () => {
          remaining -= 1;
          if (remaining <= 0) this.uploading.set(false);
        },
      });
    });
    input.value = '';
  }

  setPrimary(photo: StagedBusinessPhoto): void {
    this.stagedPhotos.update((list) => list.map((p) => ({ ...p, isPrimary: p.url === photo.url })));
  }

  removeStaged(photo: StagedBusinessPhoto): void {
    this.stagedPhotos.update((list) => list.filter((p) => p.url !== photo.url));
  }
}

interface BusinessFormModel {
  typeCode: string;
  name: string;
  description: string;
  phone: string;
  email: string;
  address: string;
  municipalityCode: string;
  latitude: number | null;
  longitude: number | null;
  countryCode: string;
  cityCode: string;
}
