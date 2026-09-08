from __future__ import annotations
import pandas as pd
import requests

from ..cache import cached
from ..config import settings

BASE_URL = "https://api.twelvedata.com/time_series"


def enabled() -> bool:
    return bool(settings.twelve_data_api_key)


def history(symbol: str, interval: str = "1day", outputsize: int = 5000) -> pd.DataFrame:
    if not enabled():
        raise RuntimeError("TWELVE_DATA_API_KEY non configurata")

    symbol = symbol.upper().strip()
    key = f"twelve:history:{symbol}:{interval}:{outputsize}"

    def load():
        params = {
            "symbol": symbol,
            "interval": interval,
            "outputsize": min(int(outputsize), 5000),
            "apikey": settings.twelve_data_api_key,
        }
        r = requests.get(BASE_URL, params=params, timeout=20)
        r.raise_for_status()
        data = r.json()
        if data.get("status") == "error":
            raise RuntimeError(data.get("message", "Twelve Data error"))
        values = data.get("values", [])
        if not values:
            raise ValueError(f"Twelve Data: nessun dato per {symbol}")
        df = pd.DataFrame(values)
        df["datetime"] = pd.to_datetime(df["datetime"])
        for c in ["open", "high", "low", "close", "volume"]:
            if c in df.columns:
                df[c] = pd.to_numeric(df[c], errors="coerce")
        return df.set_index("datetime").sort_index().rename(
            columns={"open":"Open", "high":"High", "low":"Low", "close":"Close", "volume":"Volume"}
        )

    return cached(key, 15 * 60, load)
