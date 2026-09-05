from typing import Optional, Literal
from pydantic import BaseModel, Field


class ExtendedTraits(BaseModel):
    age: Optional[int] = None
    dwelling: Optional[Literal["apartment", "house"]] = None
    dwelling_size: Optional[Literal["small", "medium", "large"]] = None
    has_yard: Optional[bool] = None
    has_kids: Optional[bool] = None
    kids_ages: Optional[Literal["toddlers", "young", "teenagers"]] = None
    maintenance_pref: Optional[Literal["low", "medium", "high"]] = None
    activity_level: Optional[Literal["sedentary", "moderate", "active"]] = None
    time_availability: Optional[Literal["low", "medium", "high"]] = None
    experience_level: Optional[Literal["beginner", "intermediate", "experienced"]] = None
    budget: Optional[Literal["low", "medium", "high"]] = None
    allergies: Optional[bool] = None
    work_schedule: Optional[Literal["away_all_day", "flexible", "home_based"]] = None
    climate: Optional[Literal["tropical", "temperate", "cold"]] = None
    other_pets: Optional[bool] = None


class PetMatch(BaseModel):
    id: str
    name: str
    species_code: str
    breed_code: Optional[str] = None
    score: int = Field(description="0-100 compatibility score")
    match_percentage: float = Field(description="Percentage match (0-100)")
    reasons: list[str] = Field(description="Why this pet matches")
    concerns: list[str] = Field(default_factory=list, description="Potential issues")
    notes: str


class RecommendRequest(BaseModel):
    prompt: str = Field(min_length=5, max_length=2000, description="User's description of lifestyle")


class RecommendResponse(BaseModel):
    understood_traits: ExtendedTraits
    extraction_method: Literal["llm", "fallback_regex"]
    top_match: PetMatch
    alternatives: list[PetMatch]
    confidence: float = Field(description="Overall confidence in recommendation (0-1)")
