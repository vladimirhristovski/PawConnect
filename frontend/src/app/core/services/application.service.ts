import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Application, ApplicationDecision, CreateApplicationRequest } from '../models/application';
import { Page } from '../models/page';

@Service()
export class ApplicationService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  submit(listingId: number, request: CreateApplicationRequest) {
    return this.http.post<Application>(
      `${this.baseUrl}/listings/${listingId}/applications`,
      request,
    );
  }
  getForListing(listingId: number, page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Application>>(`${this.baseUrl}/listings/${listingId}/applications`, {
      params,
    });
  }
  getMine(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Application>>(`${this.baseUrl}/applications/mine`, { params });
  }
  review(id: number, decision: ApplicationDecision) {
    const params = new HttpParams().set('decision', decision);
    return this.http.patch<Application>(
      `${this.baseUrl}/applications/${id}/review`,
      {},
      { params },
    );
  }
  withdraw(id: number) {
    return this.http.post<Application>(`${this.baseUrl}/applications/${id}/withdraw`, {});
  }
}
