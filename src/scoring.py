from __future__ import annotations
import math
import numpy as np
import pandas as pd


def _clip01(x):
    if x is None or (isinstance(x, float) and math.isnan(x)):
        return None
    return float(np.clip(x, 0, 1))


def _higher(x, bad, good):
    if x is None: return None
    if good == bad: return 0.5
    return _clip01((x - bad) / (good - bad))


def _lower(x, good, bad):
    if x is None: return None
    if bad == good: return 0.5
    return _clip01((bad - x) / (bad - good))


def _mean(values):
    vals = [v for v in values if v is not None and not pd.isna(v)]
    return None if not vals else float(np.mean(vals))


def build_factor_scores(info: dict, tech: dict, sec: dict | None = None) -> dict:
    sec = sec or {}
    pe = info.get("trailingPE")
    ev_ebitda = info.get("enterpriseToEbitda")
    pb = info.get("priceToBook")
    fcf = info.get("freeCashflow")
    mcap = info.get("marketCap")
    fcf_yield = (fcf / mcap) if fcf and mcap else None

    valuation = _mean([
        _lower(pe, 12, 35),
        _lower(ev_ebitda, 8, 25),
        _lower(pb, 1.5, 8),
        _higher(fcf_yield, 0.02, 0.08),
    ])

    roe = info.get("returnOnEquity")
    margin = info.get("profitMargins")
    debt_equity = info.get("debtToEquity")
    ocf = sec.get("operating_cash_flow")
    ni = sec.get("net_income")
    cash_conversion = (ocf / ni) if ocf is not None and ni not in (None, 0) and ni > 0 else None

    quality = _mean([
        _higher(roe, 0.05, 0.25),
        _higher(margin, 0.03, 0.20),
        _lower(debt_equity, 30, 180),
        _higher(cash_conversion, 0.7, 1.3),
    ])

    growth = _mean([
        _higher(info.get("revenueGrowth"), 0.00, 0.20),
        _higher(info.get("earningsGrowth"), 0.00, 0.25),
    ])

    momentum = _mean([
        _higher(tech.get("momentum_3m"), -0.10, 0.20),
        _higher(tech.get("momentum_6m"), -0.15, 0.35),
        _higher(tech.get("momentum_12m"), -0.20, 0.50),
    ])

    trend = _mean([
        _higher((tech["price"] / tech["sma50"] - 1) if tech.get("sma50") else None, -0.10, 0.15),
        _higher((tech["price"] / tech["sma200"] - 1) if tech.get("sma200") else None, -0.15, 0.25),
        _higher((tech["macd"] - tech["macd_signal"]) / tech["price"] if tech.get("price") else None, -0.01, 0.01),
    ])

    risk = _mean([
        _lower(tech.get("volatility_annual"), 0.15, 0.60),
        _lower(abs(tech.get("max_drawdown")) if tech.get("max_drawdown") is not None else None, 0.15, 0.55),
        _lower(tech.get("atr14_pct"), 0.015, 0.06),
    ])

    factors = {
        "valuation": valuation,
        "quality": quality,
        "growth": growth,
        "momentum": momentum,
        "trend": trend,
        "risk": risk,
    }

    weights = {"valuation":0.20, "quality":0.25, "growth":0.15, "momentum":0.20, "trend":0.10, "risk":0.10}
    available = [(k, v) for k, v in factors.items() if v is not None]
    if available:
        denom = sum(weights[k] for k, _ in available)
        composite = sum(v * weights[k] for k, v in available) / denom
    else:
        composite = None

    return {
        **{k: round(v*100, 1) if v is not None else None for k,v in factors.items()},
        "composite": round(composite*100, 1) if composite is not None else None,
        "pe": pe, "ev_ebitda": ev_ebitda, "pb": pb, "fcf_yield": fcf_yield,
        "roe": roe, "profit_margin": margin, "debt_to_equity": debt_equity,
        "revenue_growth": info.get("revenueGrowth"), "earnings_growth": info.get("earningsGrowth")
    }
