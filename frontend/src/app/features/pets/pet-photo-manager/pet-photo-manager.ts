import { Component, inject, input, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PetService } from '../../../core/services/pet';
import { LookupService } from '../../../core/services/lookup';
import { UpdatePetRequest } from '../../../core/models/pet.model';

@Component({
  selector: 'app-pet-photo-manager',
  imports: [FormsModule, RouterLink],
  templateUrl: './pet-photo-manager.html',
  styleUrl: './pet-photo-manager.css'
})
export class PetPhotoManager {
  protected petService = inject(PetService);
  protected lookup = inject(LookupService);

  id = input.required<string>();

  model: UpdatePetRequest = {};
  saving = signal(false);
  saveError = signal<string | null>(null);
  saveSuccess = signal(false);

  uploading = signal(false);
  uploadError = signal<string | null>(null);

  constructor() {
    this.lookup.loadSpecies();

    effect(() => {
      this.petService.loadOne(Number(this.id()));
    });

    effect(() => {
      const pet = this.petService.selected();
      if (pet) {
        this.model = {
          name: pet.name,
          speciesCode: pet.speciesCode,
          breedCodes: pet.breeds.map(b => b.code),
          gender: pet.gender,
          size: pet.size ?? undefined,
          age: pet.age ?? undefined,
          birthDate: pet.birthDate ?? undefined,
          weightKg: pet.weightKg ?? undefined,
          description: pet.description ?? undefined,
          goodWithKids: pet.goodWithKids,
          goodWithOtherPets: pet.goodWithOtherPets
        };
        this.lookup.loadBreeds(pet.speciesCode);
      }
    });
  }

  onSpeciesChange(): void {
    this.model.breedCodes = [];
    if (this.model.speciesCode) this.lookup.loadBreeds(this.model.speciesCode);
  }

  isBreedSelected(code: string): boolean { return (this.model.breedCodes ?? []).includes(code); }

  toggleBreed(code: string): void {
    const current = this.model.breedCodes ?? [];
    this.model.breedCodes = current.includes(code) ? current.filter(c => c !== code) : [...current, code];
  }

  saveDetails(): void {
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.saving.set(true);
    this.petService.update(Number(this.id()), this.model).subscribe({
      next: () => { this.saving.set(false); this.saveSuccess.set(true); this.petService.loadOne(Number(this.id())); },
      error: err => { this.saving.set(false); this.saveError.set(err.error?.message ?? 'Could not save pet details.'); }
    });
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
      next: () => { this.uploading.set(false); this.petService.loadOne(Number(this.id())); },
      error: err => { this.uploading.set(false); this.uploadError.set(err.error?.message ?? 'Upload failed.'); }
    });
    input.value = '';
  }

  removePhoto(photoId: number): void {
    if (!confirm('Remove this photo?')) return;
    this.petService.removePhoto(Number(this.id()), photoId).subscribe(() => this.petService.loadOne(Number(this.id())));
  }
}