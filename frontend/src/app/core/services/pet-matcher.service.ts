import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { PetMatcherResponse } from '../models/pet-matcher';

@Service()
export class PetMatcherService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/pet-matcher`;

  recommend(prompt: string) {
    return this.http.post<PetMatcherResponse>(`${this.baseUrl}/recommend`, { prompt });
  }
}
