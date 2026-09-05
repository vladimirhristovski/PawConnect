import { Service, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Species,
  Breed,
  BusinessType,
  Country,
  City,
  Municipality,
  ListingStatus,
  ApplicationStatus,
} from '../models/lookup';
import { Role } from '../models/user';

@Service()
export class LookupService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/lookups`;

  species = signal<Species[]>([]);
  breeds = signal<Breed[]>([]);
  businessTypes = signal<BusinessType[]>([]);
  countries = signal<Country[]>([]);
  cities = signal<City[]>([]);
  municipalities = signal<Municipality[]>([]);
  listingStatuses = signal<ListingStatus[]>([]);
  applicationStatuses = signal<ApplicationStatus[]>([]);
  roles = signal<Role[]>([]);

  loadSpecies(): void {
    this.http.get<Species[]>(`${this.baseUrl}/species`).subscribe((data) => this.species.set(data));
  }
  loadBreeds(speciesCode?: string): void {
    let params = new HttpParams();
    if (speciesCode) {
      params = params.set('speciesCode', speciesCode);
    }
    this.http
      .get<Breed[]>(`${this.baseUrl}/breeds`, { params })
      .subscribe((data) => this.breeds.set(data));
  }
  loadBusinessTypes(): void {
    this.http
      .get<BusinessType[]>(`${this.baseUrl}/business-types`)
      .subscribe((data) => this.businessTypes.set(data));
  }
  loadCountries(): void {
    this.http
      .get<Country[]>(`${this.baseUrl}/countries`)
      .subscribe((data) => this.countries.set(data));
  }
  loadCities(countryCode?: string): void {
    let params = new HttpParams();
    if (countryCode) {
      params = params.set('countryCode', countryCode);
    }
    this.http
      .get<City[]>(`${this.baseUrl}/cities`, { params })
      .subscribe((data) => this.cities.set(data));
  }
  loadMunicipalities(cityCode?: string): void {
    let params = new HttpParams();
    if (cityCode) {
      params = params.set('cityCode', cityCode);
    }
    this.http
      .get<Municipality[]>(`${this.baseUrl}/municipalities`, { params })
      .subscribe((data) => this.municipalities.set(data));
  }
  loadListingStatuses(): void {
    this.http
      .get<ListingStatus[]>(`${this.baseUrl}/listing-statuses`)
      .subscribe((data) => this.listingStatuses.set(data));
  }
  loadApplicationStatuses(): void {
    this.http
      .get<ApplicationStatus[]>(`${this.baseUrl}/application-statuses`)
      .subscribe((data) => this.applicationStatuses.set(data));
  }
  loadRoles(): void {
    this.http.get<Role[]>(`${this.baseUrl}/roles`).subscribe((data) => this.roles.set(data));
  }
}
