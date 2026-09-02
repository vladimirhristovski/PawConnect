import { Component, inject, input, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BusinessService } from '../../../core/services/business';
import { LookupService } from '../../../core/services/lookup';
import { UpdateBusinessRequest } from '../../../core/models/business.model';

@Component({
  selector: 'app-business-photo-manager',
  imports: [FormsModule, RouterLink],
  templateUrl: './business-photo-manager.html',
  styleUrl: './business-photo-manager.css',
})
export class BusinessPhotoManager {
  protected businessService = inject(BusinessService);
  protected lookup = inject(LookupService);

  id = input.required<string>();

  model: UpdateBusinessRequest = {};
  saving = signal(false);
  saveError = signal<string | null>(null);
  saveSuccess = signal(false);

  uploading = signal(false);
  uploadError = signal<string | null>(null);

  constructor() {
    this.lookup.loadBusinessTypes();

    effect(() => {
      this.businessService.loadOne(Number(this.id()));
    });

    effect(() => {
      const biz = this.businessService.selected();
      if (biz) {
        this.model = {
          typeCode: biz.typeCode,
          name: biz.name,
          description: biz.description ?? undefined,
          phone: biz.phone,
          email: biz.email ?? undefined,
          address: biz.address,
          municipalityCode: biz.municipalityCode,
          latitude: biz.latitude ?? undefined,
          longitude: biz.longitude ?? undefined,
        };
        this.lookup.loadMunicipalities();
      }
    });
  }

  saveDetails(): void {
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.saving.set(true);
    this.businessService.update(Number(this.id()), this.model).subscribe({
      next: () => {
        this.saving.set(false);
        this.saveSuccess.set(true);
        this.businessService.loadOne(Number(this.id()));
      },
      error: (err) => {
        this.saving.set(false);
        this.saveError.set(err.error?.detail ?? 'Could not save business details.');
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadError.set(null);
    this.uploading.set(true);
    const biz = this.businessService.selected();
    const nextOrder = biz ? biz.photos.length : 0;
    const isPrimary = biz ? biz.photos.length === 0 : true;

    this.businessService.uploadPhoto(Number(this.id()), file, isPrimary, nextOrder).subscribe({
      next: () => {
        this.uploading.set(false);
        this.businessService.loadOne(Number(this.id()));
      },
      error: (err) => {
        this.uploading.set(false);
        this.uploadError.set(err.error?.detail ?? 'Upload failed.');
      },
    });
    input.value = '';
  }

  removePhoto(photoId: number): void {
    if (!confirm('Remove this photo?')) return;
    this.businessService
      .removePhoto(Number(this.id()), photoId)
      .subscribe(() => this.businessService.loadOne(Number(this.id())));
  }
}
