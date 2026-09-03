import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Business,
  BusinessPhoto,
  BusinessPhotoRequest,
  CreateBusinessRequest,
  UpdateBusinessRequest,
  BusinessSearchParams,
} from '../models/business';
import { Page } from '../models/page';

@Service()
export class BusinessService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/businesses`;

  search(params: BusinessSearchParams) {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<Page<Business>>(this.baseUrl, { params: httpParams });
  }
  getById(id: number) {
    return this.http.get<Business>(`${this.baseUrl}/${id}`);
  }
  create(request: CreateBusinessRequest) {
    return this.http.post<Business>(this.baseUrl, request);
  }
  update(id: number, request: UpdateBusinessRequest) {
    return this.http.put<Business>(`${this.baseUrl}/${id}`, request);
  }
  delete(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
  addPhotoByUrl(id: number, request: BusinessPhotoRequest) {
    return this.http.post<BusinessPhoto>(`${this.baseUrl}/${id}/photos`, request);
  }
  uploadPhoto(id: number, file: File, isPrimary: boolean, displayOrder: number) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPrimary', String(isPrimary));
    formData.append('displayOrder', String(displayOrder));
    return this.http.post<BusinessPhoto>(`${this.baseUrl}/${id}/photos/upload`, formData);
  }
  removePhoto(id: number, photoId: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}/photos/${photoId}`);
  }
}
