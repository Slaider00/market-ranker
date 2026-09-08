from __future__ import annotations
import pandas as pd

from .config import settings
from .indicators import technical_snapshot
from .scoring import build_factor_scores
from .providers import yahoo, twelve, sec


def get_history(symbol: str, prefer_twelve: bool = False) -> tuple[pd.DataFrame, str]:
    if prefer_twelve and twelve.enabled():
        try:
            return twelve.history(symbol), "Twelve Data"
        except Exception:
            pass
    return yahoo.history(symbol), "Yahoo/yfinance"


def analyze_symbol(symbol: str, prefer_twelve: bool = False) -> dict:
    symbol = symbol.upper().strip()
    history, source = get_history(symbol, prefer_twelve=prefer_twelve)
    tech = technical_snapshot(history)
    info = yahoo.company_info(symbol)

    sec_data = {}
    try:
        sec_data = sec.standardized_fundamentals(symbol)
    except Exception:
        sec_data = {}

    scores = build_factor_scores(info, tech, sec_data)
    return {
        "symbol": symbol,
        "name": info.get("shortName") or info.get("longName") or symbol,
        "sector": info.get("sector"),
        "industry": info.get("industry"),
        "currency": info.get("currency"),
        "price_source": source,
        **tech,
        **scores,
    }


def rank_symbols(symbols: list[str], prefer_twelve: bool = False) -> tuple[pd.DataFrame, list[str]]:
    unique = []
    for s in symbols:
        s = s.strip().upper()
        if s and s not in unique:
            unique.append(s)

    if prefer_twelve and twelve.enabled():
        unique = unique[:settings.max_twelve_symbols_per_run]

    rows, errors = [], []
    for symbol in unique:
        try:
            rows.append(analyze_symbol(symbol, prefer_twelve=prefer_twelve))
        except Exception as e:
            errors.append(f"{symbol}: {e}")

    df = pd.DataFrame(rows)
    if not df.empty and "composite" in df.columns:
        df = df.sort_values("composite", ascending=False, na_position="last").reset_index(drop=True)
        df.insert(0, "rank", range(1, len(df) + 1))
    return df, errors
