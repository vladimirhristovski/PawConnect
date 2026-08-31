import { Component, inject, input, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ListingService } from '../../../core/services/listing';
import { LookupService } from '../../../core/services/lookup';
import { PhotoService } from '../../../core/services/photo';
import { CreateListingRequest, UpdateListingRequest } from '../../../core/models/listing.model';
import { CreatePetRequest, PetPhotoRequest } from '../../../core/models/pet.model';
import { Coordinates } from '../../../core/models/coordinates.model';
import { getCurrentPosition } from '../../../shared/geo/geo-utils';
import { MapPicker } from '../../../shared/map-picker/map-picker';

interface StagedPhoto extends PetPhotoRequest {
  previewName: string;
}

@Component({
  selector: 'app-listing-form',
  imports: [FormsModule, MapPicker],
  templateUrl: './listing-form.html',
  styleUrl: './listing-form.css',
})
export class ListingForm {
  protected listingService = inject(ListingService);
  protected lookup = inject(LookupService);
  private photoService = inject(PhotoService);
  private router = inject(Router);

  id = input<string>();

  pet: CreatePetRequest = {
    name: '',
    speciesCode: '',
    gender: 'UNKNOWN',
    breedCodes: [],
    goodWithKids: false,
    goodWithOtherPets: false,
  };
  listing: Omit<CreateListingRequest, 'pet' | 'municipalityCode'> & { municipalityCode?: string } =
    { adoptionFee: 0 };
  expiresAtDate = '';
  saveAsDraft = false;
  countryCode?: string;
  cityCode?: string;

  stagedPhotos = signal<StagedPhoto[]>([]);
  uploading = signal(false);
  submitting = signal(false);
  error = signal<string | null>(null);

  locating = signal(false);
  locationError = signal<string | null>(null);
  manualLocation = false;
  showMapPicker = signal(false);

  constructor() {
    this.lookup.loadSpecies();
    this.lookup.loadCountries();

    effect(() => {
      const idParam = this.id();
      if (idParam) this.listingService.loadOne(Number(idParam));
    });

    effect(() => {
      const existing = this.listingService.selected();
      if (existing && this.isEdit()) {
        this.listing.title = existing.title ?? undefined;
        this.listing.description = existing.description ?? undefined;
        this.listing.adoptionFee = existing.adoptionFee;
        this.listing.municipalityCode = existing.municipalityCode;
        this.listing.latitude = existing.latitude ?? undefined;
        this.listing.longitude = existing.longitude ?? undefined;
        this.expiresAtDate = existing.expiresAt ? existing.expiresAt.substring(0, 10) : '';
      }
    });
  }

  isEdit(): boolean {
    return !!this.id();
  }

  onSpeciesChange(): void {
    this.pet.breedCodes = [];
    if (this.pet.speciesCode) this.lookup.loadBreeds(this.pet.speciesCode);
  }

  isBreedSelected(code: string): boolean {
    return (this.pet.breedCodes ?? []).includes(code);
  }

  toggleBreed(code: string): void {
    const current = this.pet.breedCodes ?? [];
    this.pet.breedCodes = current.includes(code)
      ? current.filter((c) => c !== code)
      : [...current, code];
  }

  onCountryChange(): void {
    this.cityCode = undefined;
    this.listing.municipalityCode = undefined;
    this.lookup.loadCities(this.countryCode);
  }

  onCityChange(): void {
    this.listing.municipalityCode = undefined;
    this.lookup.loadMunicipalities(this.cityCode);
  }

  useMyLocation(): void {
    this.locationError.set(null);
    this.locating.set(true);
    getCurrentPosition()
      .then((coords) => {
        this.locating.set(false);
        this.listing.latitude = coords.lat;
        this.listing.longitude = coords.lng;
      })
      .catch((err: Error) => {
        this.locating.set(false);
        this.locationError.set(err.message);
      });
  }

  clearLocation(): void {
    this.listing.latitude = undefined;
    this.listing.longitude = undefined;
  }

  currentCoordinates(): Coordinates | null {
    if (this.listing.latitude == null || this.listing.longitude == null) return null;
    return { lat: this.listing.latitude, lng: this.listing.longitude };
  }

  openMapPicker(): void {
    this.showMapPicker.set(true);
  }

  onMapConfirmed(coords: Coordinates): void {
    this.listing.latitude = coords.lat;
    this.listing.longitude = coords.lng;
    this.showMapPicker.set(false);
  }

  onMapCancelled(): void {
    this.showMapPicker.set(false);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files || files.length === 0) return;

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
        complete: () => {
          remaining -= 1;
          if (remaining <= 0) this.uploading.set(false);
        },
      });
    });
    input.value = '';
  }

  setPrimary(photo: StagedPhoto): void {
    this.stagedPhotos.update((list) => list.map((p) => ({ ...p, isPrimary: p.url === photo.url })));
  }

  removeStaged(photo: StagedPhoto): void {
    this.stagedPhotos.update((list) => list.filter((p) => p.url !== photo.url));
  }

  submit(): void {
    this.error.set(null);
    if (!this.listing.municipalityCode) {
      this.error.set('Please select a municipality.');
      return;
    }
    this.submitting.set(true);

    if (this.isEdit()) {
      const payload: UpdateListingRequest = {
        title: this.listing.title,
        description: this.listing.description,
        adoptionFee: this.listing.adoptionFee,
        municipalityCode: this.listing.municipalityCode,
        latitude: this.listing.latitude,
        longitude: this.listing.longitude,
        expiresAt: this.expiresAtDate ? new Date(this.expiresAtDate).toISOString() : undefined,
      };
      this.listingService.update(Number(this.id()), payload).subscribe({
        next: (updated) => this.router.navigate(['/listings', updated.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.message ?? 'Could not save changes.');
        },
      });
    } else {
      const payload: CreateListingRequest = {
        pet: { ...this.pet, photos: this.stagedPhotos().map(({ previewName, ...rest }) => rest) },
        municipalityCode: this.listing.municipalityCode,
        title: this.listing.title,
        description: this.listing.description,
        adoptionFee: this.listing.adoptionFee,
        latitude: this.listing.latitude,
        longitude: this.listing.longitude,
        expiresAt: this.expiresAtDate ? new Date(this.expiresAtDate).toISOString() : undefined,
        saveAsDraft: this.saveAsDraft,
      };
      this.listingService.create(payload).subscribe({
        next: (created) => this.router.navigate(['/listings', created.id]),
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err.error?.message ?? 'Could not create listing.');
        },
      });
    }
  }
}
