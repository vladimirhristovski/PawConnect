export interface Species {
  code: string;
  name: string;
}
export interface Breed {
  code: string;
  name: string;
  speciesCode: string;
}
export interface BusinessType {
  code: string;
  name: string;
}
export interface Country {
  code: string;
  name: string;
}
export interface City {
  code: string;
  name: string;
  countryCode: string | null;
}
export interface Municipality {
  code: string;
  name: string;
  cityCode: string | null;
}
