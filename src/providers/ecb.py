from __future__ import annotations
from io import StringIO
import pandas as pd
import requests

from ..cache import cached

BASE = "https://data-api.ecb.europa.eu/service/data"


def series(dataflow: str, key: str, last_n: int = 120) -> pd.DataFrame:
    dataflow = dataflow.strip()
    key = key.strip()

    def load():
        url = f"{BASE}/{dataflow}/{key}"
        params = {"format": "csvdata", "lastNObservations": int(last_n)}
        r = requests.get(url, params=params, timeout=25)
        r.raise_for_status()
        return pd.read_csv(StringIO(r.text))

    return cached(f"ecb:{dataflow}:{key}:{last_n}", 6 * 60 * 60, load)


def eur_usd_monthly(last_n: int = 24) -> pd.DataFrame:
    return series("EXR", "M.USD.EUR.SP00.A", last_n=last_n)
