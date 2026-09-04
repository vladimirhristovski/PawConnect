import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import RecommendRequest, RecommendResponse, ExtendedTraits
from app.llm_extractor import extract_traits_llm, LLMExtractionError
from app.fallback_extractor import extract_traits_fallback, fill_missing
from app.scorer import rank_pets, calculate_confidence

logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.app_title,
    description=settings.app_description,
    version="0.1.0",
    debug=settings.debug,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health():
    return {"status": "ok", "version": "0.1.0"}


@app.post("/recommend", response_model=RecommendResponse)
async def recommend(payload: RecommendRequest):
    """
    Recommend pets based on free text lifestyle description.
    
    Extraction pipeline:
    1. Try LLM for nuanced trait extraction
    2. Fall back to regex if LLM unavailable or fails
    3. Backfill missing fields from regex as safety net
    4. Score all pets against extracted traits
    5. Return ranked recommendations with confidence score
    """
    prompt = payload.prompt
    logger.info(f"Processing recommendation request: {prompt[:50]}...")

    extraction_method = "llm"
    try:
        logger.info("Attempting LLM trait extraction...")
        traits = await extract_traits_llm(prompt)
    except LLMExtractionError as e:
        logger.warning(f"LLM extraction failed, falling back to regex: {e}")
        traits = extract_traits_fallback(prompt)
        extraction_method = "fallback_regex"
    else:
        logger.info("LLM extraction succeeded, backfilling with regex...")
        fallback_traits = extract_traits_fallback(prompt)
        traits = fill_missing(traits, fallback_traits)

    logger.info(f"Extracted traits: {traits}")

    # Score and rank all pets
    ranked = rank_pets(traits)
    logger.info(f"Ranked {len(ranked)} pets. Top 3: {[p['name'] for p in ranked[:3]]}")

    top_match, *alternatives = ranked

    # Calculate recommendation confidence
    confidence = calculate_confidence(traits, ranked)
    logger.info(f"Recommendation confidence: {confidence}")

    return RecommendResponse(
        understood_traits=ExtendedTraits(**traits),
        extraction_method=extraction_method,
        top_match=top_match,
        alternatives=alternatives[:3],
        confidence=confidence,
    )
