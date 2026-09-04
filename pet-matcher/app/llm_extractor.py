"""
Ollama-backed LLM trait extractor for extended trait set.
"""
import json
import os
from dotenv import load_dotenv
import httpx
import logging

load_dotenv()
logger = logging.getLogger(__name__)

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:1.5b")

SYSTEM_PROMPT = """You extract detailed pet-owner traits from a user's message.
Respond ONLY with a JSON object matching exactly this schema, no other text:

{
  "age": <int or null>,
  "dwelling": <"apartment" or "house" or null>,
  "dwelling_size": <"small", "medium", "large" or null>,
  "has_yard": <true, false, or null>,
  "has_kids": <true, false, or null>,
  "kids_ages": <"toddlers", "young", "teenagers" or null>,
  "maintenance_pref": <"low", "medium", "high" or null>,
  "activity_level": <"sedentary", "moderate", "active" or null>,
  "time_availability": <"low", "medium", "high" or null>,
  "experience_level": <"beginner", "intermediate", "experienced" or null>,
  "budget": <"low", "medium", "high" or null>,
  "allergies": <true, false, or null>,
  "work_schedule": <"away_all_day", "flexible", "home_based" or null>,
  "climate": <"tropical", "temperate", "cold" or null>,
  "other_pets": <true, false, or null>
}

Rules:
- Use null for anything not mentioned or unclear.
- "don't have kids" -> has_kids: false
- "have 2 kids" -> has_kids: true
- Only output the JSON object, nothing else, no markdown.
- dwelling_size can be inferred: apartment="small", house="large"
"""

REQUIRED_FIELDS = {
    "age", "dwelling", "dwelling_size", "has_yard", "has_kids", "kids_ages",
    "maintenance_pref", "activity_level", "time_availability", "experience_level",
    "budget", "allergies", "work_schedule", "climate", "other_pets"
}


class LLMExtractionError(Exception):
    pass


async def extract_traits_llm(prompt, timeout=15.0):
    """Call Ollama and return an extended traits dict. Raises LLMExtractionError on failure."""
    payload = {
        "model": OLLAMA_MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        "format": "json",
        "stream": False,
    }

    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(f"{OLLAMA_HOST}/api/chat", json=payload)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        raise LLMExtractionError(f"Could not reach Ollama at {OLLAMA_HOST}: {exc}") from exc

    try:
        content = resp.json()["message"]["content"]
        data = json.loads(content)
    except (KeyError, json.JSONDecodeError) as exc:
        raise LLMExtractionError(f"Ollama returned unparseable output: {exc}") from exc

    if not REQUIRED_FIELDS.issubset(data.keys()):
        missing = REQUIRED_FIELDS - set(data.keys())
        raise LLMExtractionError(f"Ollama response missing fields: {missing}")

    return data
