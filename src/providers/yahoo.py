from __future__ import annotations
import pandas as pd
import yfinance as yf

from ..cache import cached

PRICE_TTL = 15 * 60
INFO_TTL = 12 * 60 * 60


def history(symbol: str, period: str = "2y", interval: str = "1d") -> pd.DataFrame:
    symbol = symbol.upper().strip()
    key = f"yf:history:{symbol}:{period}:{interval}"

    def load():
        df = yf.download(
            symbol,
            period=period,
            interval=interval,
            auto_adjust=True,
            progress=False,
            threads=False,
        )
        if df.empty:
            raise ValueError(f"Nessun dato prezzo trovato per {symbol}")
        if isinstance(df.columns, pd.MultiIndex):
            df.columns = df.columns.get_level_values(0)
        df.index = pd.to_datetime(df.index)
        return df.sort_index()

    return cached(key, PRICE_TTL, load)


def company_info(symbol: str) -> dict:
    symbol = symbol.upper().strip()
    key = f"yf:info:{symbol}"

    def load():
        obj = yf.Ticker(symbol)
        try:
            info = obj.get_info()
        except Exception:
            info = obj.info
        return info or {}

    return cached(key, INFO_TTL, load)
