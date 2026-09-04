"""
Regex/keyword-based fallback trait extractor.
Extracts traits like activity level, work schedule, budget, allergies, etc.
"""
import re

NEGATION_PATTERNS = {
    "has_yard": [r"no\s+yard", r"don'?t\s+have\s+a?\s*yard", r"without\s+a\s+yard"],
    "has_kids": [r"no\s+kids?", r"don'?t\s+have\s+(a\s+)?kids?", r"dont\s+have\s+kid"],
    "allergies": [r"allerg", r"allergy"],
}

POSITIVE_PATTERNS = {
    "has_yard": [r"\byard\b", r"\bgarden\b", r"\bbackyard\b"],
    "has_kids": [
        r"\bkids?\b", r"\bchildren\b", r"\btoddlers?\b", r"\bteens?\b",
        r"\bteenagers?\b", r"\binfants?\b",
    ],
    "allergies": [r"allerg"],
    "other_pets": [r"other\s+pets?", r"dogs?|cats?|birds?", r"already\s+have"],
}

DWELLING_PATTERNS = {
    "apartment": [r"apartment", r"\bflat\b", r"\bcondo\b", r"studio"],
    "house": [r"\bhouse\b", r"home\s+with"],
}

DWELLING_SIZE_PATTERNS = {
    "small": [r"small\s+apartment", r"studio", r"tiny"],
    "large": [r"large\s+(house|home)", r"big\s+(house|home)"],
}

MAINTENANCE_PATTERNS = {
    "low": [r"low.?maintenance", r"easy\s+to\s+(take\s+care|care\s+for)", r"not\s+much\s+time", r"busy", r"minimal"],
    "high": [r"high.?maintenance", r"lots?\s+of\s+attention", r"needy"],
    "medium": [r"medium.?maintenance", r"some\s+attention"],
}

ACTIVITY_PATTERNS = {
    "active": [r"active", r"exercise", r"workout", r"run", r"hike", r"outdoor"],
    "sedentary": [r"lazy", r"couch", r"home.?body", r"quiet", r"low\s+energy"],
}

TIME_AVAILABILITY_PATTERNS = {
    "low": [r"busy", r"working", r"away.*all.*day", r"minimal\s+time", r"not\s+much\s+time"],
    "high": [r"lots?\s+of\s+time", r"can\s+dedicate", r"flexible", r"home\s+based", r"retired"],
}

BUDGET_PATTERNS = {
    # Checked in this order — "high" signals checked first so phrases like
    # "budget is not an issue" don't get caught by the generic "budget" match below.
    "high": [
        r"budget\s+is\s+not\s+(an\s+)?issue", r"budget\s+is\s+not\s+a\s+problem",
        r"money\s+is\s+not\s+(an\s+)?issue", r"don'?t\s+mind\s+spending",
        r"no\s+budget\s+constraints?", r"cost\s+is\s+not\s+(an\s+)?issue",
    ],
    "low": [
        r"tight\s+budget", r"limited\s+budget", r"low\s+budget", r"\bcheap\b",
        r"can'?t\s+afford", r"cost.?conscious", r"budget.?conscious", r"on\s+a\s+budget",
    ],
}

WORK_SCHEDULE_PATTERNS = {
    "away_all_day": [r"work.*all.*day", r"away\s+(all\s+)?day", r"9\s*[-–]\s*5", r"busy.*work"],
    "home_based": [
        r"work\s+from\s+home", r"remote", r"home.?based",
        r"home\s+(most|all)\s+of\s+the\s+(day|time)", r"home\s+all\s+day",
    ],
}

EXPERIENCE_PATTERNS = {
    "beginner": [r"first.*pet", r"never.*owned", r"new\s+to", r"beginner"],
    "experienced": [r"experience", r"owned.*before", r"familiar", r"handled"],
}

AGE_PATTERN = re.compile(r"(\d{1,3})\s*(years?|yrs?|y/?o)\b")

KID_AGE_PATTERNS = {
    "toddlers": [r"toddler", r"baby", r"infant", r"small\s+child"],
    "young": [r"young\s+child", r"elementary", r"school\s+age"],
    "teenagers": [r"teen", r"adolescent"],
}


def _match_any(patterns, text):
    return any(re.search(p, text, re.IGNORECASE) for p in patterns)


def extract_traits_fallback(prompt):
    text = prompt.lower()
    traits = {
        "age": None,
        "dwelling": None,
        "dwelling_size": None,
        "has_yard": None,
        "has_kids": None,
        "kids_ages": None,
        "maintenance_pref": None,
        "activity_level": None,
        "time_availability": None,
        "experience_level": None,
        "budget": None,
        "allergies": None,
        "work_schedule": None,
        "other_pets": None,
    }

    # Age
    age_match = AGE_PATTERN.search(text)
    if age_match:
        traits["age"] = int(age_match.group(1))

    # Dwelling
    for value, patterns in DWELLING_PATTERNS.items():
        if _match_any(patterns, text):
            traits["dwelling"] = value
            break

    # Dwelling size
    for value, patterns in DWELLING_SIZE_PATTERNS.items():
        if _match_any(patterns, text):
            traits["dwelling_size"] = value
            break

    # Has yard / needs yard
    if _match_any(NEGATION_PATTERNS["has_yard"], text):
        traits["has_yard"] = False
    elif _match_any(POSITIVE_PATTERNS["has_yard"], text):
        traits["has_yard"] = True

    # Kids
    if _match_any(NEGATION_PATTERNS["has_kids"], text):
        traits["has_kids"] = False
    elif _match_any(POSITIVE_PATTERNS["has_kids"], text):
        traits["has_kids"] = True
        # Try to infer kids' ages
        for age_group, patterns in KID_AGE_PATTERNS.items():
            if _match_any(patterns, text):
                traits["kids_ages"] = age_group
                break

    # Allergies
    if _match_any(POSITIVE_PATTERNS["allergies"], text):
        traits["allergies"] = True

    # Maintenance
    for value, patterns in MAINTENANCE_PATTERNS.items():
        if _match_any(patterns, text):
            traits["maintenance_pref"] = value
            break

    # Activity level
    for value, patterns in ACTIVITY_PATTERNS.items():
        if _match_any(patterns, text):
            traits["activity_level"] = value
            break

    # Time availability
    for value, patterns in TIME_AVAILABILITY_PATTERNS.items():
        if _match_any(patterns, text):
            traits["time_availability"] = value
            break

    # Budget
    for value, patterns in BUDGET_PATTERNS.items():
        if _match_any(patterns, text):
            traits["budget"] = value
            break

    # Work schedule
    for value, patterns in WORK_SCHEDULE_PATTERNS.items():
        if _match_any(patterns, text):
            traits["work_schedule"] = value
            break

    # Experience
    for value, patterns in EXPERIENCE_PATTERNS.items():
        if _match_any(patterns, text):
            traits["experience_level"] = value
            break

    # Other pets
    if _match_any(POSITIVE_PATTERNS["other_pets"], text):
        traits["other_pets"] = True

    return traits


def fill_missing(primary, fallback):
    merged = dict(primary)
    for key, value in fallback.items():
        if merged.get(key) is None:
            merged[key] = value
    return merged
