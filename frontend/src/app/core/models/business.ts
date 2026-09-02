export interface Business {
  id: number;
  typeCode: string;
  typeName: string;
  name: string;
  description: string | null;
  phone: string;
  email: string | null;
  address: string;
  municipalityCode: string;
  municipalityName: string;
  ownerUsername: string | null;
  latitude: number | null;
  longitude: number | null;
  photos: BusinessPhoto[];
}

export interface CreateBusinessRequest {
  typeCode: string;
  name: string;
  description?: string;
  phone: string;
  email?: string;
  address: string;
  municipalityCode: string;
  latitude?: number;
  longitude?: number;
  photos?: BusinessPhotoRequest[];
}

export interface UpdateBusinessRequest {
  typeCode?: string;
  name?: string;
  description?: string;
  phone?: string;
  email?: string;
  address?: string;
  municipalityCode?: string;
  latitude?: number;
  longitude?: number;
}

export interface BusinessSearchParams {
  typeCode?: string;
  municipalityCode?: string;
  lat?: number;
  lng?: number;
  radiusKm?: number;
  page?: number;
  size?: number;
}

export interface BusinessPhoto {
  id: number;
  url: string;
  isPrimary: boolean;
  displayOrder: number;
}

export interface BusinessPhotoRequest {
  url: string;
  isPrimary?: boolean;
  displayOrder?: number;
}

export interface StagedBusinessPhoto extends BusinessPhotoRequest {
  previewName: string;
}
