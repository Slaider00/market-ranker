from __future__ import annotations

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
import streamlit as st

from src.config import settings
from src.engine import rank_symbols
from src.providers import fred


st.set_page_config(
    page_title="Market Ranker",
    page_icon="MR",
    layout="wide",
    initial_sidebar_state="collapsed",
)

st.markdown(
    """
    <style>
      :root {
        --mr-border: rgba(128,128,128,.20);
        --mr-muted: rgba(128,128,128,.92);
        --mr-soft: rgba(128,128,128,.08);
        --mr-positive: #16a34a;
        --mr-warning: #d97706;
        --mr-negative: #dc2626;
      }

      #MainMenu, footer { visibility: hidden; }
      .block-container {
        max-width: 1160px;
        padding-top: 1.3rem;
        padding-bottom: 3rem;
      }

      h1, h2, h3 { letter-spacing: -0.025em; }
      h1 { margin-bottom: .15rem; }

      .mr-header {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: .45rem;
      }
      .mr-kicker {
        font-size: .75rem;
        letter-spacing: .11em;
        text-transform: uppercase;
        font-weight: 700;
        opacity: .62;
        margin-bottom: .25rem;
      }
      .mr-title {
        font-size: 2rem;
        line-height: 1.05;
        font-weight: 760;
        letter-spacing: -.04em;
      }
      .mr-subtitle {
        margin-top: .35rem;
        font-size: .92rem;
        opacity: .68;
      }
      .mr-status {
        border: 1px solid var(--mr-border);
        border-radius: 999px;
        padding: .38rem .72rem;
        font-size: .78rem;
        white-space: nowrap;
        opacity: .82;
      }

      .mr-card {
        border: 1px solid var(--mr-border);
        border-radius: 16px;
        padding: 1rem 1.05rem;
        background: var(--mr-soft);
      }
      .mr-card-title {
        font-size: .75rem;
        text-transform: uppercase;
        letter-spacing: .08em;
        opacity: .58;
        font-weight: 700;
      }
      .mr-card-value {
        font-size: 1.7rem;
        line-height: 1.1;
        font-weight: 760;
        margin-top: .28rem;
      }
      .mr-card-meta {
        margin-top: .35rem;
        font-size: .82rem;
        opacity: .65;
      }

      .rank-card {
        border: 1px solid var(--mr-border);
        border-radius: 16px;
        padding: .95rem 1rem;
        margin-bottom: .65rem;
        background: rgba(128,128,128,.035);
      }
      .rank-topline {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        gap: .75rem;
      }
      .rank-symbol {
        font-size: 1.02rem;
        font-weight: 760;
      }
      .rank-score {
        font-size: 1.22rem;
        font-weight: 760;
      }
      .rank-name {
        font-size: .86rem;
        opacity: .66;
        margin-top: .08rem;
      }
      .rank-meta {
        display: flex;
        justify-content: space-between;
        gap: .75rem;
        margin-top: .55rem;
        font-size: .79rem;
        opacity: .64;
      }

      .score-good { color: var(--mr-positive); }
      .score-mid { color: var(--mr-warning); }
      .score-low { color: var(--mr-negative); }

      [data-testid="stMetric"] {
        border: 1px solid var(--mr-border);
        border-radius: 14px;
        padding: .78rem .85rem;
        background: rgba(128,128,128,.035);
      }
      [data-testid="stMetricLabel"] { opacity: .66; }

      div[data-testid="stButton"] > button {
        min-height: 46px;
        border-radius: 12px;
        font-weight: 650;
      }

      textarea { font-size: 16px !important; }
      .stTabs [data-baseweb="tab-list"] {
        gap: .2rem;
        border-bottom: 1px solid var(--mr-border);
      }
      .stTabs [data-baseweb="tab"] {
        padding-left: .75rem;
        padding-right: .75rem;
      }

      .small-note {
        opacity: .62;
        font-size: .79rem;
        line-height: 1.45;
      }

      @media (max-width: 760px) {
        .block-container {
          padding-left: .75rem;
          padding-right: .75rem;
          padding-top: .75rem;
        }
        .mr-header {
          display: block;
        }
        .mr-title { font-size: 1.75rem; }
        .mr-status {
          display: inline-block;
          margin-top: .65rem;
        }
        .rank-card { padding: .85rem .9rem; }
        .stTabs [data-baseweb="tab-list"] {
          overflow-x: auto;
          white-space: nowrap;
        }
        [data-testid="stHorizontalBlock"] {
          flex-wrap: wrap;
          gap: .5rem;
        }
        [data-testid="stHorizontalBlock"] > div[data-testid="stColumn"] {
          flex: 1 1 46% !important;
          min-width: 145px !important;
          width: auto !important;
        }
      }
    </style>
    """,
    unsafe_allow_html=True,
)


def score_text(value) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value):.0f}"


def score_class(value) -> str:
    if value is None or pd.isna(value):
        return ""
    value = float(value)
    if value >= 70:
        return "score-good"
    if value >= 50:
        return "score-mid"
    return "score-low"


def pct_text(value) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value) * 100:.1f}%"


def num_text(value, decimals: int = 2) -> str:
    return "N/D" if value is None or pd.isna(value) else f"{float(value):.{decimals}f}"


def price_text(row: pd.Series) -> str:
    if row.get("price") is None or pd.isna(row.get("price")):
        return "N/D"
    return f"{float(row.get('price')):.2f} {row.get('currency') or ''}".strip()


def rank_card(row: pd.Series) -> None:
    sector = row.get("sector") or "Settore non disponibile"
    st.markdown(
        f"""
        <div class="rank-card">
          <div class="rank-topline">
            <div class="rank-symbol">#{int(row['rank'])}&nbsp;&nbsp;{row['symbol']}</div>
            <div class="rank-score {score_class(row.get('composite'))}">{score_text(row.get('composite'))}/100</div>
          </div>
          <div class="rank-name">{row.get('name') or row['symbol']}</div>
          <div class="rank-meta">
            <span>{price_text(row)}</span>
            <span>{sector}</span>
          </div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def factor_chart(row: pd.Series) -> go.Figure:
    labels = ["Quality", "Value", "Momentum", "Growth", "Trend", "Risk"]
    keys = ["quality", "valuation", "momentum", "growth", "trend", "risk"]
    values = [row.get(k) if not pd.isna(row.get(k)) else None for k in keys]

    fig = go.Figure(
        go.Bar(
            x=values,
            y=labels,
            orientation="h",
            text=[f"{v:.0f}" if v is not None else "N/D" for v in values],
            textposition="outside",
            hovertemplate="%{y}: %{x:.1f}/100<extra></extra>",
        )
    )
    fig.update_layout(
        height=300,
        margin=dict(l=8, r=35, t=8, b=8),
        xaxis=dict(range=[0, 105], title="", showgrid=True),
        yaxis=dict(title="", autorange="reversed"),
        showlegend=False,
    )
    return fig


if "ranking_df" not in st.session_state:
    st.session_state.ranking_df = pd.DataFrame()
if "ranking_errors" not in st.session_state:
    st.session_state.ranking_errors = []


st.markdown(
    f"""
    <div class="mr-header">
      <div>
        <div class="mr-kicker">Equity Research</div>
        <div class="mr-title">Market Ranker</div>
        <div class="mr-subtitle">Screening multifattoriale di azioni ed ETF, con metodologia trasparente.</div>
      </div>
      <div class="mr-status">Free-only mode: {"ON" if settings.free_only else "OFF"}</div>
    </div>
    """,
    unsafe_allow_html=True,
)

with st.expander("Dati e configurazione", expanded=False):
    c1, c2 = st.columns(2)
    with c1:
        prefer_twelve = st.toggle(
            "Preferisci Twelve Data per i prezzi",
            value=False,
            disabled=not bool(settings.twelve_data_api_key),
            help="Se non configurato, il motore usa Yahoo/yfinance.",
        )
    with c2:
        st.markdown(
            f"""
            **Provider**
            
            Twelve Data: {"attivo" if settings.twelve_data_api_key else "non configurato"}  
            FRED: {"attivo" if settings.fred_api_key else "non configurato"}  
            Limite Twelve Data/run: {settings.max_twelve_symbols_per_run}
            """
        )

DEFAULT = "AAPL, MSFT, GOOGL, META, AMZN, NVDA, TTWO, ASML, SAP.DE, RHM.DE, BMPS.MI"

input_col, action_col = st.columns([4, 1])
with input_col:
    tickers = st.text_area(
        "Universo da analizzare",
        value=DEFAULT,
        height=88,
        help="Separa i ticker con virgole o vai a capo. Esempi: BMPS.MI, SAP.DE, ASML.AS.",
    )
with action_col:
    st.write("")
    st.write("")
    run = st.button("Aggiorna ranking", type="primary", use_container_width=True)

if run:
    symbols = [x.strip() for x in tickers.replace("\n", ",").split(",") if x.strip()]
    with st.spinner("Recupero dati e calcolo score..."):
        df_new, errors_new = rank_symbols(symbols, prefer_twelve=prefer_twelve)
    st.session_state.ranking_df = df_new
    st.session_state.ranking_errors = errors_new

df = st.session_state.ranking_df
errors = st.session_state.ranking_errors

if not df.empty:
    analyzed = len(df)
    avg_score = df["composite"].dropna().mean() if "composite" in df.columns else None
    leader = df.iloc[0]

    s1, s2, s3 = st.columns(3)
    s1.metric("Titoli analizzati", f"{analyzed}")
    s2.metric("Score medio", "N/D" if pd.isna(avg_score) else f"{avg_score:.0f}/100")
    s3.metric("Leader", f"{leader['symbol']} · {score_text(leader.get('composite'))}/100")

st.write("")
tab_rank, tab_detail, tab_macro = st.tabs(["Ranking", "Dettaglio titolo", "Macro"])

with tab_rank:
    if df.empty:
        st.info("Inserisci i ticker e premi **Aggiorna ranking**.")
    else:
        top = df.iloc[0]

        st.markdown("### Classifica")
        left, right = st.columns([1.05, 1.35])

        with left:
            for _, row in df.head(10).iterrows():
                rank_card(row)

        with right:
            chart_df = df.dropna(subset=["composite"]).copy().head(15)
            if not chart_df.empty:
                fig = px.bar(
                    chart_df.sort_values("composite"),
                    x="composite",
                    y="symbol",
                    orientation="h",
                    hover_data=["name"],
                )
                fig.update_traces(
                    hovertemplate="<b>%{y}</b><br>Score: %{x:.1f}/100<extra></extra>"
                )
                fig.update_layout(
                    xaxis=dict(title="Composite score", range=[0, 100]),
                    yaxis_title="",
                    height=max(390, len(chart_df) * 34),
                    margin=dict(l=5, r=10, t=8, b=35),
                    showlegend=False,
                )
                st.plotly_chart(fig, use_container_width=True, config={"displayModeBar": False})

            st.markdown("#### Leader")
            l1, l2, l3 = st.columns(3)
            l1.metric("Composite", f"{score_text(top.get('composite'))}/100")
            l2.metric("Quality", f"{score_text(top.get('quality'))}/100")
            l3.metric("Momentum", f"{score_text(top.get('momentum'))}/100")
            l1.metric("Valuation", f"{score_text(top.get('valuation'))}/100")
            l2.metric("Growth", f"{score_text(top.get('growth'))}/100")
            l3.metric("Risk", f"{score_text(top.get('risk'))}/100")

        with st.expander("Tabella completa", expanded=False):
            display_cols = [
                "rank", "symbol", "name", "price", "composite", "quality",
                "valuation", "momentum", "growth", "trend", "risk",
                "pe", "ev_ebitda", "fcf_yield", "price_source",
            ]
            display_cols = [c for c in display_cols if c in df.columns]
            st.dataframe(
                df[display_cols],
                use_container_width=True,
                hide_index=True,
                column_config={
                    "rank": "Rank",
                    "symbol": "Ticker",
                    "name": "Società",
                    "price": st.column_config.NumberColumn("Prezzo", format="%.2f"),
                    "composite": st.column_config.ProgressColumn("Score", min_value=0, max_value=100, format="%.0f"),
                    "quality": st.column_config.NumberColumn("Quality", format="%.0f"),
                    "valuation": st.column_config.NumberColumn("Value", format="%.0f"),
                    "momentum": st.column_config.NumberColumn("Momentum", format="%.0f"),
                    "growth": st.column_config.NumberColumn("Growth", format="%.0f"),
                    "trend": st.column_config.NumberColumn("Trend", format="%.0f"),
                    "risk": st.column_config.NumberColumn("Risk", format="%.0f"),
                    "fcf_yield": st.column_config.NumberColumn("FCF Yield", format="%.2%")
                },
            )

    if errors:
        with st.expander(f"Ticker non elaborati ({len(errors)})"):
            for err in errors:
                st.write(err)

with tab_detail:
    if df.empty:
        st.info("Calcola prima un ranking.")
    else:
        symbols_available = df["symbol"].tolist()
        chosen = st.selectbox(
            "Seleziona titolo",
            symbols_available,
            index=0,
            label_visibility="collapsed",
        )
        row = df.loc[df["symbol"] == chosen].iloc[0]

        st.markdown(f"### {row['symbol']} · {row.get('name') or row['symbol']}")
        meta = " · ".join(
            [x for x in [row.get("sector"), row.get("industry"), row.get("price_source")] if x]
        )
        if meta:
            st.caption(meta)

        h1, h2, h3 = st.columns(3)
        h1.metric("Prezzo", price_text(row))
        h2.metric("Composite", f"{score_text(row.get('composite'))}/100")
        h3.metric("Posizione", f"#{int(row['rank'])} su {len(df)}")

        st.markdown("#### Profilo fattoriale")
        factor_left, factor_right = st.columns([1.25, 1])

        with factor_left:
            st.plotly_chart(
                factor_chart(row),
                use_container_width=True,
                config={"displayModeBar": False},
            )

        with factor_right:
            q1, q2 = st.columns(2)
            q1.metric("Quality", f"{score_text(row.get('quality'))}/100")
            q2.metric("Value", f"{score_text(row.get('valuation'))}/100")
            q1.metric("Momentum", f"{score_text(row.get('momentum'))}/100")
            q2.metric("Growth", f"{score_text(row.get('growth'))}/100")
            q1.metric("Trend", f"{score_text(row.get('trend'))}/100")
            q2.metric("Risk", f"{score_text(row.get('risk'))}/100")

        st.markdown("#### Fondamentali")
        f1, f2, f3 = st.columns(3)
        f1.metric("P/E", num_text(row.get("pe")))
        f2.metric("EV / EBITDA", num_text(row.get("ev_ebitda")))
        f3.metric("FCF Yield", pct_text(row.get("fcf_yield")))
        f1.metric("ROE", pct_text(row.get("roe")))
        f2.metric("Margine netto", pct_text(row.get("profit_margin")))
        f3.metric("Debt / Equity", num_text(row.get("debt_to_equity")))

        st.markdown("#### Momentum e rischio")
        t1, t2, t3 = st.columns(3)
        t1.metric("Momentum 3M", pct_text(row.get("momentum_3m")))
        t2.metric("Momentum 12M", pct_text(row.get("momentum_12m")))
        t3.metric("RSI 14", num_text(row.get("rsi14"), 1))
        t1.metric("Volatilità ann.", pct_text(row.get("volatility_annual")))
        t2.metric("Max drawdown", pct_text(row.get("max_drawdown")))
        t3.metric("ATR 14", pct_text(row.get("atr14_pct")))

with tab_macro:
    st.markdown("### Quadro macro USA")
    st.caption("Modulo opzionale basato su FRED.")

    if settings.fred_api_key:
        if st.button("Aggiorna dati macro", use_container_width=True):
            with st.spinner("Aggiornamento FRED..."):
                st.session_state["macro_snapshot"] = fred.macro_snapshot()

        macro = st.session_state.get("macro_snapshot")
        if macro:
            m1, m2, m3 = st.columns(3)
            m1.metric("Fed Funds", num_text(macro.get("fed_funds"), 2) + "%")
            m2.metric("Treasury 10Y", num_text(macro.get("treasury_10y"), 2) + "%")
            m3.metric("Treasury 2Y", num_text(macro.get("treasury_2y"), 2) + "%")
            m1.metric("Disoccupazione", num_text(macro.get("unemployment"), 1) + "%")
            m2.metric("CPI Index", num_text(macro.get("cpi"), 1))
            m3.metric("10Y - 2Y", num_text(macro.get("yield_curve_10y_2y"), 2) + " pp")
        else:
            st.info("Premi **Aggiorna dati macro** per caricare lo snapshot.")
    else:
        st.info(
            "FRED non è configurato. Il ranking azionario continua a funzionare normalmente; "
            "la chiave serve solo per questo modulo macro."
        )

st.divider()
st.markdown(
    """
    <div class="small-note">
      Il Market Ranker è uno strumento di ricerca quantitativa, non una raccomandazione d'investimento.
      I pesi del modello sono trasparenti ma non ancora validati tramite backtest. Dati soggetti ai termini dei provider.
    </div>
    """,
    unsafe_allow_html=True,
)
