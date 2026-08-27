import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TempUploadResponse } from '../models/pet.model';

@Service()
export class PhotoService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/photos`;

  uploadTemp(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<TempUploadResponse>(`${this.baseUrl}/upload`, formData);
  }
}
