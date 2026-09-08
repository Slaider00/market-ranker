from __future__ import annotations
import hashlib
import pickle
import time
from pathlib import Path
from typing import Any, Callable

from .config import settings


def _path(key: str) -> Path:
    root = Path(settings.cache_dir)
    root.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256(key.encode("utf-8")).hexdigest()
    return root / f"{digest}.pkl"


def get(key: str, ttl_seconds: int) -> Any | None:
    p = _path(key)
    if not p.exists():
        return None
    try:
        with p.open("rb") as f:
            created, value = pickle.load(f)
        if time.time() - created <= ttl_seconds:
            return value
    except Exception:
        return None
    return None


def set_(key: str, value: Any) -> None:
    p = _path(key)
    with p.open("wb") as f:
        pickle.dump((time.time(), value), f, protocol=pickle.HIGHEST_PROTOCOL)


def cached(key: str, ttl_seconds: int, loader: Callable[[], Any]) -> Any:
    value = get(key, ttl_seconds)
    if value is not None:
        return value
    value = loader()
    set_(key, value)
    return value
