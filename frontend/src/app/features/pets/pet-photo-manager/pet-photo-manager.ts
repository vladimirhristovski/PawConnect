import { Component, inject, input, effect, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PetService } from '../../../core/services/pet.service';
import { LookupService } from '../../../core/services/lookup.service';
import { Gender, Size, UpdatePetRequest } from '../../../core/models/pet';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-pet-photo-manager',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './pet-photo-manager.html',
  styleUrl: './pet-photo-manager.css',
})
export class PetPhotoManager {
  protected petService = inject(PetService);
  protected lookup = inject(LookupService);

  id = input.required<string>();

  detailsModel = signal<PetDetailsForm>({
    name: '',
    speciesCode: '',
    gender: 'UNKNOWN',
    size: '',
    age: null,
    birthDate: '',
    weightKg: null,
    description: '',
    goodWithKids: false,
    goodWithOtherPets: false,
  });
  breedCodes = signal<string[]>([]);
  saveSuccess = signal(false);
  saveError = signal<string | null>(null);

  uploading = signal(false);
  uploadError = signal<string | null>(null);

  detailsForm = form(
    this.detailsModel,
    (path) => {
      required(path.name, { message: 'Name is required' });
    },
    {
      submission: {
        action: async (form) => {
          this.saveError.set(null);
          this.saveSuccess.set(false);
          const value = form().value();
          const payload: UpdatePetRequest = {
            name: value.name,
            speciesCode: value.speciesCode,
            breedCodes: this.breedCodes(),
            gender: value.gender,
            size: value.size || undefined,
            age: value.age ?? undefined,
            birthDate: value.birthDate || undefined,
            weightKg: value.weightKg ?? undefined,
            description: value.description || undefined,
            goodWithKids: value.goodWithKids,
            goodWithOtherPets: value.goodWithOtherPets,
          };
          try {
            await firstValueFrom(this.petService.update(Number(this.id()), payload));
            this.saveSuccess.set(true);
            this.petService.loadOne(Number(this.id()));
          } catch (err) {
            this.saveError.set(apiErrorMessage(err, 'Could not save pet details.'));
          }
          return;
        },
      },
    },
  );

  constructor() {
    this.lookup.loadSpecies();

    effect(() => {
      this.petService.loadOne(Number(this.id()));
    });

    effect(() => {
      const pet = this.petService.selected();
      if (pet) {
        this.detailsModel.set({
          name: pet.name,
          speciesCode: pet.speciesCode,
          gender: pet.gender,
          size: pet.size ?? '',
          age: pet.age,
          birthDate: pet.birthDate ?? '',
          weightKg: pet.weightKg,
          description: pet.description ?? '',
          goodWithKids: pet.goodWithKids,
          goodWithOtherPets: pet.goodWithOtherPets,
        });
        this.breedCodes.set(pet.breeds.map((b) => b.code));
        this.lookup.loadBreeds(pet.speciesCode);
      }
    });
  }

  onSpeciesChange(): void {
    this.breedCodes.set([]);
    const speciesCode = this.detailsModel().speciesCode;
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

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadError.set(null);
    this.uploading.set(true);
    const pet = this.petService.selected();
    const nextOrder = pet ? pet.photos.length : 0;
    const isPrimary = pet ? pet.photos.length === 0 : true;

    this.petService.uploadPhoto(Number(this.id()), file, isPrimary, nextOrder).subscribe({
      next: () => {
        this.uploading.set(false);
        this.petService.loadOne(Number(this.id()));
      },
      error: (err) => {
        this.uploading.set(false);
        this.uploadError.set(apiErrorMessage(err, 'Upload failed.'));
      },
    });
    input.value = '';
  }

  removePhoto(photoId: number): void {
    if (!confirm('Remove this photo?')) return;
    this.petService
      .removePhoto(Number(this.id()), photoId)
      .subscribe(() => this.petService.loadOne(Number(this.id())));
  }
}

interface PetDetailsForm {
  name: string;
  speciesCode: string;
  gender: Gender;
  size: Size | '';
  age: number | null;
  birthDate: string;
  weightKg: number | null;
  description: string;
  goodWithKids: boolean;
  goodWithOtherPets: boolean;
}
