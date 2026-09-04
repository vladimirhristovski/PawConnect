export type ExtractionMethod = 'llm' | 'fallback_regex';

export interface ExtendedTraits {
  age: number | null;
  dwelling: 'apartment' | 'house' | null;
  dwellingSize: 'small' | 'medium' | 'large' | null;
  hasYard: boolean | null;
  hasKids: boolean | null;
  kidsAges: 'toddlers' | 'young' | 'teenagers' | null;
  maintenancePref: 'low' | 'medium' | 'high' | null;
  activityLevel: 'sedentary' | 'moderate' | 'active' | null;
  timeAvailability: 'low' | 'medium' | 'high' | null;
  experienceLevel: 'beginner' | 'intermediate' | 'experienced' | null;
  budget: 'low' | 'medium' | 'high' | null;
  allergies: boolean | null;
  workSchedule: 'away_all_day' | 'flexible' | 'home_based' | null;
  climate: 'tropical' | 'temperate' | 'cold' | null;
  otherPets: boolean | null;
}

export interface PetMatch {
  id: string;
  name: string;
  speciesCode: string;
  breedCode: string | null;
  score: number;
  matchPercentage: number;
  reasons: string[];
  concerns: string[];
  notes: string | null;
}

export interface PetMatcherResponse {
  understoodTraits: ExtendedTraits;
  extractionMethod: ExtractionMethod;
  topMatch: PetMatch;
  alternatives: PetMatch[];
  confidence: number;
}
