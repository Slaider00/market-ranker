from __future__ import annotations
import numpy as np
import pandas as pd


def _close(df: pd.DataFrame) -> pd.Series:
    return pd.to_numeric(df["Close"], errors="coerce").dropna()


def rsi(close: pd.Series, period: int = 14) -> pd.Series:
    delta = close.diff()
    gain = delta.clip(lower=0).ewm(alpha=1/period, adjust=False).mean()
    loss = (-delta.clip(upper=0)).ewm(alpha=1/period, adjust=False).mean()
    rs = gain / loss.replace(0, np.nan)
    return 100 - (100 / (1 + rs))


def technical_snapshot(df: pd.DataFrame) -> dict:
    close = _close(df)
    if len(close) < 60:
        raise ValueError("Servono almeno ~60 sedute per gli indicatori")

    ema12 = close.ewm(span=12, adjust=False).mean()
    ema26 = close.ewm(span=26, adjust=False).mean()
    macd = ema12 - ema26
    sig = macd.ewm(span=9, adjust=False).mean()
    returns = close.pct_change().dropna()

    def mom(days: int):
        return float(close.iloc[-1] / close.iloc[-days] - 1) if len(close) > days else None

    high = pd.to_numeric(df.get("High"), errors="coerce")
    low = pd.to_numeric(df.get("Low"), errors="coerce")
    prev_close = close.shift(1)
    tr = pd.concat([(high-low).abs(), (high-prev_close).abs(), (low-prev_close).abs()], axis=1).max(axis=1)
    atr14 = tr.rolling(14).mean()

    rolling_max = close.cummax()
    drawdown = close / rolling_max - 1

    return {
        "price": float(close.iloc[-1]),
        "sma20": float(close.rolling(20).mean().iloc[-1]),
        "sma50": float(close.rolling(50).mean().iloc[-1]),
        "sma200": float(close.rolling(200).mean().iloc[-1]) if len(close) >= 200 else None,
        "rsi14": float(rsi(close, 14).iloc[-1]),
        "macd": float(macd.iloc[-1]),
        "macd_signal": float(sig.iloc[-1]),
        "atr14_pct": float(atr14.iloc[-1] / close.iloc[-1]) if pd.notna(atr14.iloc[-1]) else None,
        "volatility_annual": float(returns.std() * np.sqrt(252)),
        "max_drawdown": float(drawdown.min()),
        "momentum_1m": mom(21),
        "momentum_3m": mom(63),
        "momentum_6m": mom(126),
        "momentum_12m": mom(252),
    }
