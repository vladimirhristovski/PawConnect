"""
Scoring engine:
- Weighted scoring based on trait importance
- Hard dealbreaker caps
- Soft penalties (anti-patterns)
- Cost compatibility checks
- Explainable reasoning
"""
import logging
from app.knowledge_base import PETS

logger = logging.getLogger(__name__)


def score_pet(pet, traits):
    """
    Score a pet against user traits.

    Returns: (score, reasons_good, concerns)
    """
    score = 50  # Baseline
    reasons = []
    concerns = []

    # --- Graded penalties (contribute to score + concerns) ---

    if traits.get("has_kids") is True and not pet.get("good_with_kids", False):
        score -= 30
        concerns.append("Not recommended for households with children")

    if traits.get("has_yard") is False and pet.get("needs_yard", False):
        score -= 25
        concerns.append("Requires a yard which you don't have")

    user_space = traits.get("dwelling_size") or infer_space_from_dwelling(traits)
    if user_space == "small" and pet["space_needed"] == "large":
        score -= 25
        concerns.append("Needs significantly more space than available")
    elif user_space == "small" and pet["space_needed"] == "medium":
        score -= 10
        concerns.append("Medium space needs may be tight")

    user_budget = traits.get("budget")
    pet_cost = pet.get("cost_per_month", 50)
    if user_budget == "low" and pet_cost > 60:
        score -= 20
        concerns.append(f"High monthly cost (~${pet_cost}) exceeds your budget")
    elif user_budget == "medium" and pet_cost > 100:
        score -= 10
        concerns.append(f"Monthly cost (~${pet_cost}) is above your range")

    # --- Positive scoring: alignment on multiple dimensions ---

    if pet["space_needed"] == user_space:
        score += 8
        reasons.append("Perfect space fit for your home")
    elif pet["space_needed"] == "small" and user_space in ("medium", "large"):
        score += 5
        reasons.append("Flexible with different space sizes")

    user_maintenance = traits.get("maintenance_pref")
    if user_maintenance and pet["maintenance"] == user_maintenance:
        score += 8
        reasons.append(f"{user_maintenance.title()}-maintenance pet as you prefer")
    elif user_maintenance == "low" and pet["maintenance"] == "medium":
        score -= 5
        concerns.append("Slightly higher maintenance than you're looking for")

    time_avail = traits.get("time_availability")
    if time_avail == "low" and pet["time_commitment"] in ("low", "medium"):
        score += 6
        reasons.append("Fits well with limited availability")
    elif time_avail == "high" and pet["time_commitment"] == "high":
        score += 8
        reasons.append("Matches your ability to dedicate significant time")
    elif time_avail == "low" and pet["time_commitment"] == "high":
        score -= 15
        concerns.append("Requires more daily attention than you likely have")

    activity = traits.get("activity_level")
    if activity == "active" and pet["exercise_needs"] == "high":
        score += 6
        reasons.append("Matches your active lifestyle")
    elif activity == "sedentary" and pet["exercise_needs"] in ("low", "medium"):
        score += 6
        reasons.append("Good fit for a quieter lifestyle")
    elif activity == "sedentary" and pet["exercise_needs"] == "high":
        score -= 12
        concerns.append("High exercise needs may not match sedentary lifestyle")

    exp = traits.get("experience_level")
    if exp == "beginner" and pet["training_difficulty"] == "easy":
        score += 6
        reasons.append("Easy to care for if you're new to pet ownership")
    elif exp == "beginner" and pet["training_difficulty"] == "difficult":
        score -= 12
        concerns.append("Requires experienced owner; might be challenging")
    elif exp == "experienced" and pet["training_difficulty"] == "difficult":
        score += 6
        reasons.append("Ideal for your experience level")

    if traits.get("allergies") is True:
        if not pet["allergenic"]:
            score += 12
            reasons.append("Hypoallergenic breed suits your allergies")
        else:
            score -= 15
            concerns.append("May trigger allergies")

    work = traits.get("work_schedule")
    social_needs = pet["social_needs"]
    if work == "away_all_day" and social_needs == "high":
        score -= 15
        concerns.append("High social needs; may struggle with long alone periods")
    elif work == "home_based" and social_needs == "high":
        score += 6
        reasons.append("Thrives with your presence at home")

    if traits.get("other_pets") is True:
        if pet.get("good_with_other_pets", False):
            score += 6
            reasons.append("Compatible with your existing pets")
        else:
            score -= 20
            concerns.append("Not compatible with other pets")

    if user_budget and pet_cost:
        if user_budget == "high" or (user_budget == "medium" and pet_cost < 80):
            score += 3

    if pet["lifespan_years"] > 15:
        if traits.get("activity_level") in ("moderate", "active"):
            score += 2
        else:
            score -= 2
            concerns.append(
                "Very long lifespan (~{} years) is a major commitment".format(pet["lifespan_years"])
            )

    # --- Hard dealbreaker caps ---
    # Applied last so no combination of unrelated positive traits can
    # rescue a genuinely unsafe or structurally incompatible match.
    if traits.get("has_kids") is True and not pet.get("good_with_kids", False):
        score = min(score, 18)

    if traits.get("has_yard") is False and pet.get("needs_yard", False):
        score = min(score, 30)

    if user_space == "small" and pet["space_needed"] == "large":
        score = min(score, 30)

    if user_budget == "low" and pet_cost > 100:
        score = min(score, 30)

    if traits.get("other_pets") is True and not pet.get("good_with_other_pets", False):
        score = min(score, 35)

    # Clamp to sane 0-100 display range
    score = max(0, min(100, score))

    return score, reasons, concerns


def infer_space_from_dwelling(traits):
    """Infer dwelling size from dwelling type if not explicitly given."""
    if traits.get("dwelling_size"):
        return traits["dwelling_size"]
    if traits.get("dwelling") == "apartment":
        return "small"
    elif traits.get("dwelling") == "house":
        return "large"
    return "medium"


def rank_pets(traits):
    """
    Rank all pets against user traits.

    Returns list of ranked pet dicts with score, reasons, concerns.
    """
    results = []
    for pet in PETS:
        score, reasons, concerns = score_pet(pet, traits)
        match_pct = score / 100.0

        results.append({
            "id": pet["id"],
            "name": pet["name"],
            "species_code": pet["species_code"],
            "breed_code": pet["breed_code"],
            "score": score,
            "match_percentage": round(match_pct * 100, 1),
            "reasons": reasons or ["General compatibility"],
            "concerns": concerns,
            "notes": pet["description"],
        })

    results.sort(key=lambda r: r["score"], reverse=True)

    return results


def calculate_confidence(traits, top_matches):
    """
    Calculate overall recommendation confidence (0-1).

    Lower confidence if:
    - Many traits are unknown
    - Top matches are very close in score (unclear winner)
    - Top match has several concerns attached
    """
    confidence = 0.8

    trait_count = sum(1 for v in traits.values() if v is not None)
    max_traits = len(traits)
    known_ratio = trait_count / max_traits if max_traits > 0 else 0
    if known_ratio < 0.4:
        confidence -= 0.25
    elif known_ratio < 0.6:
        confidence -= 0.12

    if top_matches and top_matches[0]["concerns"]:
        concern_count = len(top_matches[0]["concerns"])
        confidence -= min(0.12, concern_count * 0.04)

    if len(top_matches) >= 3:
        top_score = top_matches[0]["score"]
        third_score = top_matches[2]["score"]
        gap = top_score - third_score
        if gap < 10:
            confidence -= 0.05

    return round(max(0.3, min(1.0, confidence)), 2)
