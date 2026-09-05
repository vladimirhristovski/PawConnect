import { PetSummary, Pet, CreatePetRequest, Gender, Size } from './pet';
import { Business } from './business';

export type ListingStatusCode = 'DRAFT' | 'ACTIVE' | 'ADOPTED' | 'EXPIRED' | 'CANCELLED';

export interface ListingSummary {
  id: number;
  pet: PetSummary;
  postedBy: string;
  municipalityName: string;
  statusCode: ListingStatusCode;
  statusName: string;
  title: string | null;
  adoptionFee: number;
  expiresAt: string | null;
  createdAt: string;
}

export interface Listing {
  id: number;
  pet: Pet;
  postedBy: string;
  municipalityCode: string;
  municipalityName: string;
  statusCode: ListingStatusCode;
  statusName: string;
  business: Business | null;
  title: string | null;
  description: string | null;
  adoptionFee: number;
  latitude: number | null;
  longitude: number | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateListingRequest {
  petId?: number;
  pet?: CreatePetRequest;
  businessId?: number;
  municipalityCode: string;
  title?: string;
  description?: string;
  adoptionFee?: number;
  latitude?: number;
  longitude?: number;
  expiresAt?: string;
  saveAsDraft?: boolean;
}

export interface UpdateListingRequest {
  title?: string;
  description?: string;
  adoptionFee?: number;
  municipalityCode?: string;
  latitude?: number;
  longitude?: number;
  expiresAt?: string;
}

export interface ListingSearchParams {
  speciesCode?: string;
  municipalityCode?: string;
  petSize?: Size;
  gender?: Gender;
  goodWithKids?: boolean;
  goodWithOtherPets?: boolean;
  minFee?: number;
  maxFee?: number;
  lat?: number;
  lng?: number;
  radiusKm?: number;
  page?: number;
  size?: number;
}
