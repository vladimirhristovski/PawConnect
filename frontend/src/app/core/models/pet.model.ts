export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN';
export type Size = 'SMALL' | 'MEDIUM' | 'LARGE';

export interface PetPhoto {
  id: number;
  url: string;
  isPrimary: boolean;
  displayOrder: number;
}

export interface PetPhotoRequest {
  url: string;
  isPrimary?: boolean;
  displayOrder?: number;
}

export interface Breed {
  code: string;
  name: string;
  speciesCode: string;
}

export interface Pet {
  id: number;
  name: string;
  speciesCode: string;
  speciesName: string;
  breeds: Breed[];
  gender: Gender;
  size: Size | null;
  age: number | null;
  birthDate: string | null;
  weightKg: number | null;
  description: string | null;
  goodWithKids: boolean;
  goodWithOtherPets: boolean;
  photos: PetPhoto[];
}

export interface PetSummary {
  id: number;
  name: string;
  speciesName: string;
  gender: Gender;
  size: Size | null;
  primaryPhotoUrl: string | null;
}

export interface CreatePetRequest {
  name: string;
  speciesCode: string;
  breedCodes?: string[];
  gender: Gender;
  size?: Size;
  age?: number;
  birthDate?: string;
  weightKg?: number;
  description?: string;
  goodWithKids?: boolean;
  goodWithOtherPets?: boolean;
  photos?: PetPhotoRequest[];
}

export interface UpdatePetRequest {
  name?: string;
  speciesCode?: string;
  breedCodes?: string[];
  gender?: Gender;
  size?: Size;
  age?: number;
  birthDate?: string;
  weightKg?: number;
  description?: string;
  goodWithKids?: boolean;
  goodWithOtherPets?: boolean;
}

export interface TempUploadResponse {
  url: string;
}
