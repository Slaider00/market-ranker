from __future__ import annotations
import pandas as pd
import requests

from ..cache import cached
from ..config import settings

BASE = "https://api.stlouisfed.org/fred/series/observations"


def enabled() -> bool:
    return bool(settings.fred_api_key)


def series(series_id: str) -> pd.Series:
    if not enabled():
        raise RuntimeError("FRED_API_KEY non configurata")
    series_id = series_id.upper().strip()

    def load():
        params = {"series_id": series_id, "api_key": settings.fred_api_key, "file_type": "json"}
        r = requests.get(BASE, params=params, timeout=20)
        r.raise_for_status()
        obs = r.json().get("observations", [])
        s = pd.Series(
            [pd.to_numeric(x.get("value"), errors="coerce") for x in obs],
            index=pd.to_datetime([x["date"] for x in obs]),
            name=series_id,
            dtype="float64",
        ).dropna()
        return s

    return cached(f"fred:{series_id}", 6 * 60 * 60, load)


def macro_snapshot() -> dict:
    ids = {
        "fed_funds": "FEDFUNDS",
        "treasury_10y": "DGS10",
        "treasury_2y": "DGS2",
        "unemployment": "UNRATE",
        "cpi": "CPIAUCSL",
    }
    out = {}
    for label, sid in ids.items():
        try:
            s = series(sid)
            out[label] = float(s.iloc[-1]) if len(s) else None
        except Exception:
            out[label] = None
    if out.get("treasury_10y") is not None and out.get("treasury_2y") is not None:
        out["yield_curve_10y_2y"] = out["treasury_10y"] - out["treasury_2y"]
    else:
        out["yield_curve_10y_2y"] = None
    return out
