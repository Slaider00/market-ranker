from __future__ import annotations

import pandas as pd
import plotly.express as px
import streamlit as st

from src.config import settings
from src.engine import rank_symbols
from src.providers import fred


st.set_page_config(
    page_title="Market Ranker",
    page_icon="📈",
    layout="wide",
    initial_sidebar_state="collapsed",
)

st.markdown(
    """
    <style>
      .block-container { max-width: 1180px; padding-top: 1.2rem; padding-bottom: 3rem; }
      h1 { letter-spacing: -0.03em; }
      [data-testid="stMetric"] { border: 1px solid rgba(128,128,128,.22); border-radius: 14px; padding: .75rem .85rem; }
      .rank-card { border: 1px solid rgba(128,128,128,.22); border-radius: 16px; padding: 14px 16px; margin-bottom: 10px; }
      .rank-card .ticker { font-size: 1.05rem; font-weight: 700; }
      .rank-card .name { opacity: .72; font-size: .88rem; }
      .rank-card .score { font-size: 1.45rem; font-weight: 750; margin-top: .25rem; }
      .rank-card .meta { opacity: .72; font-size: .82rem; }
      .small-note { opacity: .72; font-size: .82rem; }
      div[data-testid="stButton"] > button { min-height: 44px; border-radius: 12px; }
      textarea { font-size: 16px !important; }

      @media (max-width: 760px) {
        .block-container { padding-left: .8rem; padding-right: .8rem; padding-top: .7rem; }
        h1 { font-size: 1.9rem !important; }
        h2 { font-size: 1.35rem !important; }
        h3 { font-size: 1.15rem !important; }
        [data-testid="stHorizontalBlock"] { flex-wrap: wrap; gap: .55rem; }
        [data-testid="stHorizontalBlock"] > div[data-testid="stColumn"] {
          flex: 1 1 46% !important; min-width: 145px !important; width: auto !important;
        }
        [data-testid="stMetric"] { padding: .65rem .7rem; }
        [data-testid="stDataFrame"] { font-size: .82rem; }
        .stTabs [data-baseweb="tab-list"] { overflow-x: auto; white-space: nowrap; }
      }
    </style>
    """,
    unsafe_allow_html=True,
)


def score_text(value) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value):.1f}/100"


def pct_text(value) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value) * 100:.1f}%"


def num_text(value, decimals: int = 2) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value):.{decimals}f}"


def rank_card(row: pd.Series) -> None:
    price = "N/D" if pd.isna(row.get("price")) else f"{row.get('price'):.2f}"
    currency = row.get("currency") or ""
    sector = row.get("sector") or "Settore N/D"
    st.markdown(
        f"""
        <div class="rank-card">
          <div class="ticker">#{int(row['rank'])} · {row['symbol']}</div>
          <div class="name">{row.get('name') or row['symbol']}</div>
          <div class="score">{score_text(row.get('composite'))}</div>
          <div class="meta">{price} {currency} · {sector}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


if "ranking_df" not in st.session_state:
    st.session_state.ranking_df = pd.DataFrame()
if "ranking_errors" not in st.session_state:
    st.session_state.ranking_errors = []

st.title("Market Ranker")
st.caption("Azioni ed ETF · scoring multifattoriale · fonti gratuite · nessun endpoint pay-per-use")

with st.expander("⚙️ Configurazione dati", expanded=False):
    c1, c2 = st.columns(2)
    with c1:
        prefer_twelve = st.toggle(
            "Usa Twelve Data per i prezzi",
            value=False,
            disabled=not bool(settings.twelve_data_api_key),
            help="Se disattivo o non configurato, usa Yahoo/yfinance.",
        )
    with c2:
        st.write("**FREE_ONLY:**", "attivo" if settings.free_only else "disattivo")
    st.caption(
        "Twelve Data: " + ("configurata" if settings.twelve_data_api_key else "non configurata")
        + " · FRED: " + ("configurata" if settings.fred_api_key else "non configurata")
        + f" · tetto Twelve Data/run: {settings.max_twelve_symbols_per_run}"
    )

DEFAULT = "AAPL, MSFT, GOOGL, META, AMZN, NVDA, TTWO, ASML, SAP.DE, RHM.DE, BMPS.MI"
tickers = st.text_area(
    "Ticker da analizzare",
    value=DEFAULT,
    height=95,
    help="Separali con virgole o vai a capo. Per alcuni mercati servono suffissi Yahoo, es. BMPS.MI, SAP.DE, ASML.AS.",
)

run = st.button("Calcola ranking", type="primary", use_container_width=True)
st.markdown(
    '<div class="small-note">Il modello è uno strumento di ricerca trasparente e non una raccomandazione d’investimento. I pesi vanno validati con backtest.</div>',
    unsafe_allow_html=True,
)

if run:
    symbols = [x.strip() for x in tickers.replace("\n", ",").split(",") if x.strip()]
    with st.spinner("Recupero dati e calcolo fattori..."):
        df, errors = rank_symbols(symbols, prefer_twelve=prefer_twelve)
    st.session_state.ranking_df = df
    st.session_state.ranking_errors = errors

df = st.session_state.ranking_df
errors = st.session_state.ranking_errors

tab_rank, tab_detail, tab_macro = st.tabs(["🏆 Ranking", "🔎 Dettaglio", "🌍 Macro"])

with tab_rank:
    if df.empty:
        st.info("Inserisci i ticker e premi **Calcola ranking**.")
    else:
        top = df.iloc[0]
        st.subheader(f"Leader: {top['symbol']}")
        m1, m2, m3, m4 = st.columns(4)
        m1.metric("Composite", score_text(top.get("composite")))
        m2.metric("Quality", score_text(top.get("quality")))
        m3.metric("Momentum", score_text(top.get("momentum")))
        m4.metric("Valuation", score_text(top.get("valuation")))

        st.markdown("#### Classifica rapida")
        for _, row in df.head(10).iterrows():
            rank_card(row)

        with st.expander("Tabella completa", expanded=False):
            score_cols = ["composite", "valuation", "quality", "growth", "momentum", "trend", "risk"]
            display_cols = ["rank", "symbol", "name", "price", *score_cols, "pe", "ev_ebitda", "fcf_yield", "price_source"]
            display_cols = [c for c in display_cols if c in df.columns]
            st.dataframe(df[display_cols], use_container_width=True, hide_index=True)

        chart_df = df.dropna(subset=["composite"]).copy().head(15)
        if not chart_df.empty:
            fig = px.bar(chart_df.sort_values("composite"), x="composite", y="symbol", orientation="h", hover_data=["name"])
            fig.update_layout(
                xaxis_title="Composite score (0–100)", yaxis_title="",
                height=max(360, len(chart_df) * 34), margin=dict(l=10, r=10, t=20, b=20),
            )
            st.plotly_chart(fig, use_container_width=True, config={"displayModeBar": False})

    if errors:
        with st.expander(f"Ticker non elaborati / errori ({len(errors)})"):
            for err in errors:
                st.write("•", err)

with tab_detail:
    if df.empty:
        st.info("Calcola prima un ranking.")
    else:
        symbols_available = df["symbol"].tolist()
        chosen = st.selectbox("Titolo", symbols_available, index=0)
        row = df.loc[df["symbol"] == chosen].iloc[0]

        st.subheader(f"{row['symbol']} · {row.get('name') or row['symbol']}")
        st.caption(" · ".join([x for x in [row.get("sector"), row.get("industry"), row.get("price_source")] if x]))

        p1, p2 = st.columns(2)
        p1.metric("Prezzo", "N/D" if pd.isna(row.get("price")) else f"{row.get('price'):.2f} {row.get('currency') or ''}")
        p2.metric("Composite", score_text(row.get("composite")))

        st.markdown("#### Fattori")
        a, b = st.columns(2)
        a.metric("Quality", score_text(row.get("quality"))); b.metric("Valuation", score_text(row.get("valuation")))
        a.metric("Momentum", score_text(row.get("momentum"))); b.metric("Growth", score_text(row.get("growth")))
        a.metric("Trend", score_text(row.get("trend"))); b.metric("Risk", score_text(row.get("risk")))

        st.markdown("#### Fondamentali")
        f1, f2 = st.columns(2)
        f1.metric("P/E", num_text(row.get("pe"))); f2.metric("EV/EBITDA", num_text(row.get("ev_ebitda")))
        f1.metric("FCF Yield", pct_text(row.get("fcf_yield"))); f2.metric("ROE", pct_text(row.get("roe")))
        f1.metric("Margine netto", pct_text(row.get("profit_margin"))); f2.metric("Debt / Equity", num_text(row.get("debt_to_equity")))

        st.markdown("#### Tecnica e rischio")
        t1, t2 = st.columns(2)
        t1.metric("Momentum 3M", pct_text(row.get("momentum_3m"))); t2.metric("Momentum 12M", pct_text(row.get("momentum_12m")))
        t1.metric("Volatilità ann.", pct_text(row.get("volatility_annual"))); t2.metric("Max drawdown", pct_text(row.get("max_drawdown")))
        t1.metric("RSI 14", num_text(row.get("rsi14"), 1)); t2.metric("ATR 14 %", pct_text(row.get("atr14_pct")))

with tab_macro:
    st.subheader("Snapshot macro USA")
    if settings.fred_api_key:
        if st.button("Aggiorna FRED", use_container_width=True):
            with st.spinner("Aggiornamento dati macro..."):
                st.session_state["macro_snapshot"] = fred.macro_snapshot()
        macro = st.session_state.get("macro_snapshot")
        if macro:
            st.json(macro)
        else:
            st.info("Premi **Aggiorna FRED** per caricare lo snapshot.")
    else:
        st.info("FRED è opzionale. Aggiungi `FRED_API_KEY` nei Secrets di Streamlit Cloud per Fed Funds, Treasury 2Y/10Y, CPI e disoccupazione.")

st.divider()
st.caption("Dati soggetti ai termini dei provider. Nessuna credenziale è inclusa nel progetto e il codice non effettua upgrade o pagamenti automatici.")
