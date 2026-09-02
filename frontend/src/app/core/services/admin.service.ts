import { Service, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { User } from '../models/user';
import { Listing } from '../models/listing';
import { Application } from '../models/application';
import { Page } from '../models/page';
import { UserSearchParams, UpdateUserStatusRequest } from '../models/admin';

@Service()
export class AdminService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/admin`;

  users = signal<Page<User> | null>(null);
  listings = signal<Page<Listing> | null>(null);
  applications = signal<Page<Application> | null>(null);

  searchUsers(params: UserSearchParams): void {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== ('' as unknown)) {
        httpParams = httpParams.set(key, String(value));
      }
    }
    this.http
      .get<Page<User>>(`${this.baseUrl}/users`, { params: httpParams })
      .subscribe((page) => this.users.set(page));
  }

  updateUserStatus(id: number, request: UpdateUserStatusRequest) {
    return this.http.patch<User>(`${this.baseUrl}/users/${id}/status`, request);
  }

  deleteUser(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/users/${id}`);
  }

  searchListings(status: string | undefined, page = 0, size = 20): void {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    this.http
      .get<Page<Listing>>(`${this.baseUrl}/listings`, { params })
      .subscribe((p) => this.listings.set(p));
  }

  searchApplications(status: string | undefined, page = 0, size = 20): void {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    this.http
      .get<Page<Application>>(`${this.baseUrl}/applications`, { params })
      .subscribe((p) => this.applications.set(p));
  }
}
