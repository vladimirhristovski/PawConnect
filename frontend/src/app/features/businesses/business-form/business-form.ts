import { Component, inject, input, effect, signal, computed } from '@angular/core';
import {
  disabled,
  email,
  form,
  FormField,
  FormRoot,
  maxLength,
  required,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, from, catchError, finalize, firstValueFrom, map, mergeMap } from 'rxjs';
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
import { apiErrorMessage } from '../../../core/api-error';

const UPLOAD_CONCURRENCY = 3;

const EMPTY_BUSINESS_FORM: BusinessFormModel = {
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
};

@Component({
  selector: 'app-business-form',
  imports: [FormField, FormRoot, RouterLink, MapPicker],
  templateUrl: './business-form.html',
  styleUrl: './business-form.css',
})
export class BusinessForm {
  private businessService = inject(BusinessService);
  protected lookup = inject(LookupService);
  private photoService = inject(PhotoService);
  private router = inject(Router);

  id = input<string>();
  isEdit = computed(() => !!this.id());

  formModel = signal<BusinessFormModel>({ ...EMPTY_BUSINESS_FORM });

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
      maxLength(path.name, 150, { message: 'Name must be at most 150 characters' });
      required(path.typeCode, { message: 'Type is required' });
      required(path.phone, { message: 'Phone is required' });
      maxLength(path.phone, 30, { message: 'Phone must be at most 30 characters' });
      email(path.email, { message: 'Enter a valid email' });
      maxLength(path.email, 255, { message: 'Email must be at most 255 characters' });
      required(path.address, { message: 'Address is required' });
      maxLength(path.address, 255, { message: 'Address must be at most 255 characters' });
      maxLength(path.description, 5000, { message: 'Description must be at most 5000 characters' });
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
            this.error.set(
              apiErrorMessage(
                err,
                this.isEdit() ? 'Could not save changes.' : 'Could not create business.',
              ),
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
      if (idParam) this.loadExisting(Number(idParam));
    });
  }

  private loadExisting(id: number): void {
    this.formModel.set({ ...EMPTY_BUSINESS_FORM });
    this.businessService.getById(id).subscribe({
      next: (existing) => {
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
      },
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not load business.')),
    });
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

  currentCoordinates = computed<Coordinates | null>(
    () => {
      const { latitude, longitude } = this.formModel();
      if (latitude == null || longitude == null) return null;
      return { lat: latitude, lng: longitude };
    },
    { equal: (a, b) => a?.lat === b?.lat && a?.lng === b?.lng },
  );

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

    from(Array.from(files))
      .pipe(
        mergeMap(
          (file) =>
            this.photoService.uploadTemp(file).pipe(
              map((res) => ({ url: res.url, name: file.name })),
              catchError((err) => {
                this.uploadError.set(apiErrorMessage(err, 'Upload failed.'));
                return EMPTY;
              }),
            ),
          UPLOAD_CONCURRENCY,
        ),
        finalize(() => this.uploading.set(false)),
      )
      .subscribe(({ url, name }) => {
        const isFirst = this.stagedPhotos().length === 0;
        this.stagedPhotos.update((list) => [
          ...list,
          { url, isPrimary: isFirst, displayOrder: list.length, previewName: name },
        ]);
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
