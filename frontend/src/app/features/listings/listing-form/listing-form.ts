import { Component, inject, input, effect, signal, computed } from '@angular/core';
import {
  disabled,
  form,
  FormField,
  FormRoot,
  maxLength,
  min,
  required,
  validate,
} from '@angular/forms/signals';
import { Router } from '@angular/router';
import { EMPTY, from, catchError, finalize, firstValueFrom, map, mergeMap } from 'rxjs';
import { ListingService } from '../../../core/services/listing.service';
import { LookupService } from '../../../core/services/lookup.service';
import { PhotoService } from '../../../core/services/photo.service';
import { CreateListingRequest, UpdateListingRequest } from '../../../core/models/listing';
import { CreatePetRequest, Gender, Size, StagedPetPhoto } from '../../../core/models/pet';
import { Coordinates } from '../../../core/models/coordinates';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import { MapPicker } from '../../../shared/map-picker/map-picker';
import { apiErrorMessage } from '../../../core/api-error';

const UPLOAD_CONCURRENCY = 3;

const EMPTY_LISTING_FORM: ListingFormModel = {
  petName: '',
  speciesCode: '',
  gender: 'UNKNOWN',
  size: '',
  age: null,
  birthDate: '',
  weightKg: null,
  goodWithKids: false,
  goodWithOtherPets: false,
  petDescription: '',
  title: '',
  listingDescription: '',
  adoptionFee: 0,
  expiresAtDate: '',
  countryCode: '',
  cityCode: '',
  municipalityCode: '',
  latitude: null,
  longitude: null,
  manualLocation: false,
  saveAsDraft: false,
};

@Component({
  selector: 'app-listing-form',
  imports: [FormField, FormRoot, MapPicker],
  templateUrl: './listing-form.html',
  styleUrl: './listing-form.css',
})
export class ListingForm {
  private listingService = inject(ListingService);
  protected lookup = inject(LookupService);
  private photoService = inject(PhotoService);
  private router = inject(Router);

  id = input<string>();
  isEdit = computed(() => !!this.id());

  formModel = signal<ListingFormModel>({ ...EMPTY_LISTING_FORM });
  breedCodes = signal<string[]>([]);

  stagedPhotos = signal<StagedPetPhoto[]>([]);
  uploading = signal(false);
  uploadError = signal<string | null>(null);
  error = signal<string | null>(null);

  locating = signal(false);
  locationError = signal<string | null>(null);
  showMapPicker = signal(false);

  listingForm = form(
    this.formModel,
    (path) => {
      required(path.petName, { message: 'Name is required', when: () => !this.isEdit() });
      maxLength(path.petName, 100, { message: 'Name must be at most 100 characters' });
      required(path.speciesCode, { message: 'Species is required', when: () => !this.isEdit() });
      required(path.municipalityCode, { message: 'Municipality is required' });
      maxLength(path.title, 150, { message: 'Title must be at most 150 characters' });
      maxLength(path.listingDescription, 5000, {
        message: 'Description must be at most 5000 characters',
      });
      maxLength(path.petDescription, 5000, {
        message: 'Description must be at most 5000 characters',
      });
      min(path.age, 0, { message: 'Age cannot be negative' });
      min(path.weightKg, 0, { message: 'Weight cannot be negative' });
      min(path.adoptionFee, 0, { message: 'Fee cannot be negative' });
      disabled(path.cityCode, { when: ({ valueOf }) => !valueOf(path.countryCode) });
      validate(path.expiresAtDate, ({ value }) => {
        const picked = value();
        if (picked && new Date(picked).getTime() <= Date.now()) {
          return { kind: 'future', message: 'Expiry date must be in the future' };
        }
        return null;
      });
    },
    {
      submission: {
        action: async (form) => {
          this.error.set(null);
          const value = form().value();
          try {
            if (this.isEdit()) {
              const payload: UpdateListingRequest = {
                title: value.title || undefined,
                description: value.listingDescription || undefined,
                adoptionFee: value.adoptionFee,
                municipalityCode: value.municipalityCode,
                latitude: value.latitude ?? undefined,
                longitude: value.longitude ?? undefined,
                expiresAt: value.expiresAtDate
                  ? new Date(value.expiresAtDate).toISOString()
                  : undefined,
              };
              const updated = await firstValueFrom(
                this.listingService.update(Number(this.id()), payload),
              );
              this.router.navigate(['/listings', updated.id]);
            } else {
              const pet: CreatePetRequest = {
                name: value.petName,
                speciesCode: value.speciesCode,
                gender: value.gender,
                breedCodes: this.breedCodes(),
                size: value.size || undefined,
                age: value.age ?? undefined,
                birthDate: value.birthDate || undefined,
                weightKg: value.weightKg ?? undefined,
                description: value.petDescription || undefined,
                goodWithKids: value.goodWithKids,
                goodWithOtherPets: value.goodWithOtherPets,
                photos: this.stagedPhotos().map(({ previewName, ...rest }) => rest),
              };
              const payload: CreateListingRequest = {
                pet,
                municipalityCode: value.municipalityCode,
                title: value.title || undefined,
                description: value.listingDescription || undefined,
                adoptionFee: value.adoptionFee,
                latitude: value.latitude ?? undefined,
                longitude: value.longitude ?? undefined,
                expiresAt: value.expiresAtDate
                  ? new Date(value.expiresAtDate).toISOString()
                  : undefined,
                saveAsDraft: value.saveAsDraft,
              };
              const created = await firstValueFrom(this.listingService.create(payload));
              this.router.navigate(['/listings', created.id]);
            }
          } catch (err) {
            this.error.set(
              apiErrorMessage(
                err,
                this.isEdit() ? 'Could not save changes.' : 'Could not create listing.',
              ),
            );
          }
          return;
        },
      },
    },
  );

  constructor() {
    this.lookup.loadSpecies();
    this.lookup.loadCountries();

    effect(() => {
      const idParam = this.id();
      if (idParam) this.loadExisting(Number(idParam));
    });
  }

  private loadExisting(id: number): void {
    this.formModel.set({ ...EMPTY_LISTING_FORM });
    this.listingService.getById(id).subscribe({
      next: (existing) => {
        this.formModel.update((m) => ({
          ...m,
          title: existing.title ?? '',
          listingDescription: existing.description ?? '',
          adoptionFee: existing.adoptionFee,
          municipalityCode: existing.municipalityCode,
          latitude: existing.latitude ?? null,
          longitude: existing.longitude ?? null,
          expiresAtDate: existing.expiresAt ? existing.expiresAt.substring(0, 10) : '',
        }));
        this.lookup.loadMunicipalities();
      },
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not load listing.')),
    });
  }

  onSpeciesChange(): void {
    this.breedCodes.set([]);
    const speciesCode = this.formModel().speciesCode;
    if (speciesCode) this.lookup.loadBreeds(speciesCode);
  }

  isBreedSelected(code: string): boolean {
    return this.breedCodes().includes(code);
  }

  toggleBreed(code: string): void {
    this.breedCodes.update((current) =>
      current.includes(code) ? current.filter((c) => c !== code) : [...current, code],
    );
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

  clearLocation(): void {
    this.formModel.update((m) => ({ ...m, latitude: null, longitude: null }));
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

  setPrimary(photo: StagedPetPhoto): void {
    this.stagedPhotos.update((list) => list.map((p) => ({ ...p, isPrimary: p.url === photo.url })));
  }

  removeStaged(photo: StagedPetPhoto): void {
    this.stagedPhotos.update((list) => list.filter((p) => p.url !== photo.url));
  }
}

interface ListingFormModel {
  petName: string;
  speciesCode: string;
  gender: Gender;
  size: Size | '';
  age: number | null;
  birthDate: string;
  weightKg: number | null;
  goodWithKids: boolean;
  goodWithOtherPets: boolean;
  petDescription: string;
  title: string;
  listingDescription: string;
  adoptionFee: number;
  expiresAtDate: string;
  countryCode: string;
  cityCode: string;
  municipalityCode: string;
  latitude: number | null;
  longitude: number | null;
  manualLocation: boolean;
  saveAsDraft: boolean;
}
