import { Component, inject, input, effect, signal } from '@angular/core';
import { email, form, FormField, FormRoot, maxLength, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { finalize, firstValueFrom } from 'rxjs';
import { BusinessService } from '../../../core/services/business.service';
import { LookupService } from '../../../core/services/lookup.service';
import { Business, UpdateBusinessRequest } from '../../../core/models/business';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-business-photo-manager',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './business-photo-manager.html',
  styleUrl: './business-photo-manager.css',
})
export class BusinessPhotoManager {
  private businessService = inject(BusinessService);
  protected lookup = inject(LookupService);

  id = input.required<string>();

  business = signal<Business | null>(null);
  loading = signal(true);
  loadError = signal<string | null>(null);

  detailsModel = signal<BusinessDetailsForm>({
    typeCode: '',
    name: '',
    description: '',
    phone: '',
    email: '',
    address: '',
    municipalityCode: '',
  });
  saveSuccess = signal(false);
  saveError = signal<string | null>(null);

  uploading = signal(false);
  uploadError = signal<string | null>(null);

  detailsForm = form(
    this.detailsModel,
    (path) => {
      required(path.name, { message: 'Name is required' });
      maxLength(path.name, 150, { message: 'Name must be at most 150 characters' });
      maxLength(path.phone, 30, { message: 'Phone must be at most 30 characters' });
      email(path.email, { message: 'Enter a valid email' });
      maxLength(path.email, 255, { message: 'Email must be at most 255 characters' });
      maxLength(path.address, 255, { message: 'Address must be at most 255 characters' });
      maxLength(path.description, 5000, { message: 'Description must be at most 5000 characters' });
    },
    {
      submission: {
        action: async (form) => {
          this.saveError.set(null);
          this.saveSuccess.set(false);
          const value = form().value();
          const payload: UpdateBusinessRequest = {
            typeCode: value.typeCode,
            name: value.name,
            description: value.description || undefined,
            phone: value.phone,
            email: value.email || undefined,
            address: value.address,
            municipalityCode: value.municipalityCode,
          };
          try {
            const updated = await firstValueFrom(
              this.businessService.update(Number(this.id()), payload),
            );
            this.applyBusiness(updated);
            this.saveSuccess.set(true);
          } catch (err) {
            this.saveError.set(apiErrorMessage(err, 'Could not save business details.'));
          }
          return;
        },
      },
    },
  );

  constructor() {
    this.lookup.loadBusinessTypes();
    effect(() => this.load(Number(this.id())));
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadError.set(null);
    this.uploading.set(true);
    const biz = this.business();
    const nextOrder = biz ? biz.photos.length : 0;
    const isPrimary = biz ? biz.photos.length === 0 : true;

    this.businessService
      .uploadPhoto(Number(this.id()), file, isPrimary, nextOrder)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: () => this.load(Number(this.id())),
        error: (err) => this.uploadError.set(apiErrorMessage(err, 'Upload failed.')),
      });
    input.value = '';
  }

  removePhoto(photoId: number): void {
    if (!confirm('Remove this photo?')) return;
    this.businessService
      .removePhoto(Number(this.id()), photoId)
      .subscribe(() => this.load(Number(this.id())));
  }

  private load(id: number): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.businessService.getById(id).subscribe({
      next: (biz) => {
        this.applyBusiness(biz);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(apiErrorMessage(err, 'Could not load business.'));
        this.loading.set(false);
      },
    });
  }

  private applyBusiness(biz: Business): void {
    this.business.set(biz);
    this.detailsModel.set({
      typeCode: biz.typeCode,
      name: biz.name,
      description: biz.description ?? '',
      phone: biz.phone,
      email: biz.email ?? '',
      address: biz.address,
      municipalityCode: biz.municipalityCode,
    });
    this.lookup.loadMunicipalities();
  }
}

interface BusinessDetailsForm {
  typeCode: string;
  name: string;
  description: string;
  phone: string;
  email: string;
  address: string;
  municipalityCode: string;
}
