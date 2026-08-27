import { Service, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Application,
  ApplicationDecision,
  CreateApplicationRequest,
} from '../models/application.model';
import { Page } from '../models/page.model';

@Service()
export class ApplicationService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  forListing = signal<Page<Application> | null>(null);
  mine = signal<Page<Application> | null>(null);

  submit(listingId: number, request: CreateApplicationRequest) {
    return this.http.post<Application>(
      `${this.baseUrl}/listings/${listingId}/applications`,
      request,
    );
  }
  loadForListing(listingId: number, page = 0, size = 20): void {
    const params = new HttpParams().set('page', page).set('size', size);
    this.http
      .get<Page<Application>>(`${this.baseUrl}/listings/${listingId}/applications`, { params })
      .subscribe((p) => this.forListing.set(p));
  }
  loadMine(page = 0, size = 20): void {
    const params = new HttpParams().set('page', page).set('size', size);
    this.http
      .get<Page<Application>>(`${this.baseUrl}/applications/mine`, { params })
      .subscribe((p) => this.mine.set(p));
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
