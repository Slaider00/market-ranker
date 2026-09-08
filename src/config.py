from __future__ import annotations
import os
from dataclasses import dataclass
from dotenv import load_dotenv

load_dotenv()


def _bool(name: str, default: bool = True) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    twelve_data_api_key: str = os.getenv("TWELVE_DATA_API_KEY", "").strip()
    fred_api_key: str = os.getenv("FRED_API_KEY", "").strip()
    sec_user_agent: str = os.getenv(
        "SEC_USER_AGENT", "MarketRanker/0.1 research@example.com"
    ).strip()
    free_only: bool = _bool("FREE_ONLY", True)
    max_twelve_symbols_per_run: int = int(os.getenv("MAX_TWELVE_SYMBOLS_PER_RUN", "40"))
    cache_dir: str = os.getenv("CACHE_DIR", ".cache")


settings = Settings()
