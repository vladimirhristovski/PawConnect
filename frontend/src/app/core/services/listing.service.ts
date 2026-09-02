import { Service, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Listing,
  ListingSummary,
  CreateListingRequest,
  UpdateListingRequest,
  ListingSearchParams,
} from '../models/listing';
import { Page } from '../models/page';

@Service()
export class ListingService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/listings`;

  results = signal<Page<ListingSummary> | null>(null);
  myListings = signal<Page<Listing> | null>(null);
  selected = signal<Listing | null>(null);

  search(params: ListingSearchParams): void {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    this.http
      .get<Page<ListingSummary>>(this.baseUrl, { params: httpParams })
      .subscribe((page) => this.results.set(page));
  }
  loadMine(page = 0, size = 20): void {
    const params = new HttpParams().set('page', page).set('size', size);
    this.http
      .get<Page<Listing>>(`${this.baseUrl}/mine`, { params })
      .subscribe((p) => this.myListings.set(p));
  }
  loadOne(id: number): void {
    this.selected.set(null);
    this.http
      .get<Listing>(`${this.baseUrl}/${id}`)
      .subscribe((listing) => this.selected.set(listing));
  }
  create(request: CreateListingRequest) {
    return this.http.post<Listing>(this.baseUrl, request);
  }
  update(id: number, request: UpdateListingRequest) {
    return this.http.put<Listing>(`${this.baseUrl}/${id}`, request);
  }
  publish(id: number) {
    return this.http.post<Listing>(`${this.baseUrl}/${id}/publish`, {});
  }
  cancel(id: number) {
    return this.http.post<Listing>(`${this.baseUrl}/${id}/cancel`, {});
  }
  delete(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
