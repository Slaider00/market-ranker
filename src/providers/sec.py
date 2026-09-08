from __future__ import annotations
import requests

from ..cache import cached
from ..config import settings

TICKERS_URL = "https://www.sec.gov/files/company_tickers.json"
FACTS_URL = "https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json"


def _headers() -> dict:
    return {"User-Agent": settings.sec_user_agent, "Accept-Encoding": "gzip, deflate"}


def ticker_map() -> dict[str, int]:
    def load():
        r = requests.get(TICKERS_URL, headers=_headers(), timeout=20)
        r.raise_for_status()
        raw = r.json()
        return {row["ticker"].upper(): int(row["cik_str"]) for row in raw.values()}
    return cached("sec:ticker_map", 24 * 60 * 60, load)


def company_facts(symbol: str) -> dict:
    symbol = symbol.upper().strip()
    mapping = ticker_map()
    if symbol not in mapping:
        raise KeyError(f"Ticker SEC non trovato: {symbol}")
    cik = mapping[symbol]

    def load():
        r = requests.get(FACTS_URL.format(cik=cik), headers=_headers(), timeout=25)
        r.raise_for_status()
        return r.json()
    return cached(f"sec:companyfacts:{cik}", 12 * 60 * 60, load)


def _latest_usd(facts: dict, concepts: list[str]) -> float | None:
    us_gaap = facts.get("facts", {}).get("us-gaap", {})
    for concept in concepts:
        node = us_gaap.get(concept, {})
        units = node.get("units", {})
        rows = units.get("USD", [])
        if not rows:
            continue
        annual = [x for x in rows if x.get("fp") == "FY" and x.get("form") in {"10-K", "10-K/A"}]
        if not annual:
            annual = rows
        annual = sorted(annual, key=lambda x: (x.get("end", ""), x.get("filed", "")))
        if annual:
            try:
                return float(annual[-1]["val"])
            except Exception:
                pass
    return None


def standardized_fundamentals(symbol: str) -> dict:
    facts = company_facts(symbol)
    return {
        "revenue": _latest_usd(facts, ["RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet"]),
        "net_income": _latest_usd(facts, ["NetIncomeLoss", "ProfitLoss"]),
        "operating_income": _latest_usd(facts, ["OperatingIncomeLoss"]),
        "assets": _latest_usd(facts, ["Assets"]),
        "liabilities": _latest_usd(facts, ["Liabilities"]),
        "equity": _latest_usd(facts, ["StockholdersEquity", "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"]),
        "cash": _latest_usd(facts, ["CashAndCashEquivalentsAtCarryingValue", "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents"]),
        "operating_cash_flow": _latest_usd(facts, ["NetCashProvidedByUsedInOperatingActivities"]),
        "capex": _latest_usd(facts, ["PaymentsToAcquirePropertyPlantAndEquipment"]),
    }
