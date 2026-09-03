import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Pet, UpdatePetRequest, PetPhoto, PetPhotoRequest } from '../models/pet';

@Service()
export class PetService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/pets`;

  getById(id: number) {
    return this.http.get<Pet>(`${this.baseUrl}/${id}`);
  }
  update(id: number, request: UpdatePetRequest) {
    return this.http.put<Pet>(`${this.baseUrl}/${id}`, request);
  }
  addPhotoByUrl(id: number, request: PetPhotoRequest) {
    return this.http.post<PetPhoto>(`${this.baseUrl}/${id}/photos`, request);
  }
  uploadPhoto(id: number, file: File, isPrimary: boolean, displayOrder: number) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPrimary', String(isPrimary));
    formData.append('displayOrder', String(displayOrder));
    return this.http.post<PetPhoto>(`${this.baseUrl}/${id}/photos/upload`, formData);
  }
  removePhoto(id: number, photoId: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}/photos/${photoId}`);
  }
}
