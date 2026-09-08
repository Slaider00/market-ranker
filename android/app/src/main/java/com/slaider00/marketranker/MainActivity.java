package com.slaider00.marketranker;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_RANKING = 1;
    private static final int SCREEN_DETAIL = 2;
    private static final int SCREEN_INFO = 3;

    private final int BG = Color.rgb(10, 14, 20);
    private final int SURFACE = Color.rgb(18, 24, 33);
    private final int SURFACE_2 = Color.rgb(24, 31, 42);
    private final int BORDER = Color.rgb(42, 53, 68);
    private final int TEXT = Color.rgb(244, 247, 251);
    private final int MUTED = Color.rgb(151, 163, 181);
    private final int BLUE = Color.rgb(67, 139, 250);
    private final int BLUE_DARK = Color.rgb(33, 86, 171);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(245, 158, 11);
    private final int RED = Color.rgb(239, 68, 68);

    private LinearLayout appRoot;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private TextView topBack;
    private TextView topTitle;
    private TextView topSubtitle;
    private TextView navHome;
    private TextView navRanking;
    private TextView navInfo;

    private EditText tickerInput;
    private TextView analyzeButton;
    private ProgressBar progress;
    private TextView status;

    private final List<StockResult> results = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private int currentScreen = SCREEN_HOME;
    private StockResult selectedResult = null;
    private String lastUniverse =
            "AAPL, MSFT, GOOGL, META, AMZN, NVDA, TTWO, ASML, SAP.DE, RHM.DE, BMPS.MI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        buildShell();
        showHome();
    }

    private void buildShell() {
        appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        appRoot.setBackgroundColor(BG);

        appRoot.addView(buildTopBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        appRoot.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        bottomNav = buildBottomNav();
        appRoot.addView(bottomNav);

        setContentView(appRoot);
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(16), dp(10));
        bar.setBackgroundColor(SURFACE);

        topBack = text("‹", 36, TEXT, false);
        topBack.setGravity(Gravity.CENTER);
        topBack.setVisibility(View.GONE);
        topBack.setBackground(rounded(SURFACE_2, 14, BORDER, 1));
        topBack.setOnClickListener(v -> navigateBack());
        bar.addView(topBack, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);

        topTitle = text("Market Ranker", 19, TEXT, true);
        topSubtitle = text("Equity research · motore locale", 11, MUTED, false);
        topSubtitle.setPadding(0, dp(1), 0, 0);

        titles.addView(topTitle);
        titles.addView(topSubtitle);
        bar.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView nativeChip = text("NATIVE", 10, BLUE, true);
        nativeChip.setGravity(Gravity.CENTER);
        nativeChip.setBackground(rounded(Color.rgb(20, 46, 78), 999, Color.rgb(45, 91, 142), 1));
        nativeChip.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.addView(nativeChip);

        return bar;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(10));
        nav.setBackgroundColor(SURFACE);

        navHome = navItem("⌂\nAnalizza", v -> showHome());
        navRanking = navItem("≡\nRanking", v -> showRanking());
        navInfo = navItem("i\nInfo", v -> showInfo());

        nav.addView(navHome, new LinearLayout.LayoutParams(0, dp(56), 1));
        nav.addView(navRanking, new LinearLayout.LayoutParams(0, dp(56), 1));
        nav.addView(navInfo, new LinearLayout.LayoutParams(0, dp(56), 1));
        return nav;
    }

    private TextView navItem(String label, View.OnClickListener listener) {
        TextView t = text(label, 12, MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(0, 0.9f);
        t.setOnClickListener(listener);
        return t;
    }

    private void setScreen(int screen, String title, String subtitle, boolean showBack) {
        currentScreen = screen;
        topTitle.setText(title);
        topSubtitle.setText(subtitle);
        topBack.setVisibility(showBack ? View.VISIBLE : View.GONE);

        styleNav(navHome, screen == SCREEN_HOME);
        styleNav(navRanking, screen == SCREEN_RANKING);
        styleNav(navInfo, screen == SCREEN_INFO);
    }

    private void styleNav(TextView item, boolean active) {
        item.setTextColor(active ? BLUE : MUTED);
        item.setBackground(active ? rounded(Color.rgb(18, 41, 69), 14, Color.TRANSPARENT, 0) : null);
    }

    private void showHome() {
        selectedResult = null;
        setScreen(SCREEN_HOME, "Market Ranker", "Costruisci il tuo universo", false);
        content.removeAllViews();

        LinearLayout hero = card(SURFACE);
        hero.addView(text("SCREENING MULTIFATTORIALE", 11, BLUE, true));

        TextView heroTitle = text("Confronta i titoli con un unico score.", 24, TEXT, true);
        heroTitle.setPadding(0, dp(6), 0, dp(8));
        hero.addView(heroTitle);

        TextView heroText = text(
                "Il motore scarica i dati, calcola indicatori e fattori direttamente sul telefono. " +
                "Tocca un titolo nel ranking per aprire l'analisi completa.",
                14, MUTED, false);
        heroText.setLineSpacing(dp(2), 1.0f);
        hero.addView(heroText);
        content.addView(hero, cardParams());

        TextView label = text("Universo da analizzare", 13, TEXT, true);
        label.setPadding(dp(2), dp(8), 0, dp(8));
        content.addView(label);

        tickerInput = new EditText(this);
        tickerInput.setText(lastUniverse);
        tickerInput.setTextColor(TEXT);
        tickerInput.setHintTextColor(MUTED);
        tickerInput.setTextSize(15);
        tickerInput.setMinLines(4);
        tickerInput.setGravity(Gravity.TOP | Gravity.START);
        tickerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tickerInput.setBackground(rounded(SURFACE, 16, BORDER, 1));
        tickerInput.setPadding(dp(14), dp(13), dp(14), dp(13));
        content.addView(tickerInput, matchWrap());

        analyzeButton = primaryButton("Aggiorna ranking", v -> analyze());
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(12);
        content.addView(analyzeButton, bp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = matchWrap();
        pp.topMargin = dp(10);
        content.addView(progress, pp);

        status = text(
                results.isEmpty()
                        ? "Pronto. I ticker possono essere separati da virgole o andare a capo."
                        : results.size() + " titoli già disponibili nella sessione.",
                12, MUTED, false);
        status.setPadding(dp(2), dp(10), 0, 0);
        content.addView(status);

        if (!results.isEmpty()) {
            LinearLayout previous = card(SURFACE_2);
            TextView p1 = text("Ultimo ranking", 13, MUTED, true);
            previous.addView(p1);
            StockResult leader = firstValid();
            if (leader != null) {
                TextView p2 = text(
                        "Leader: " + leader.symbol + " · " + fmtScore(leader.composite),
                        18, TEXT, true);
                p2.setPadding(0, dp(5), 0, dp(5));
                previous.addView(p2);
            }
            TextView open = secondaryButton("Apri classifica", v -> showRanking());
            previous.addView(open, buttonParams());
            content.addView(previous, cardParams());
        }
    }

    private void analyze() {
        String raw = tickerInput.getText().toString();
        lastUniverse = raw;
        String[] parts = raw.replace("\n", ",").split(",");
        List<String> symbols = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim().toUpperCase(Locale.ROOT);
            if (!s.isEmpty() && !symbols.contains(s)) symbols.add(s);
        }

        if (symbols.isEmpty()) {
            Toast.makeText(this, "Inserisci almeno un ticker.", Toast.LENGTH_SHORT).show();
            return;
        }

        results.clear();
        analyzeButton.setEnabled(false);
        analyzeButton.setAlpha(0.55f);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        status.setText("Avvio analisi…");

        final int total = symbols.size();
        final int[] done = {0};

        for (String symbol : symbols) {
            executor.submit(() -> {
                StockResult r;
                try {
                    r = new Analyzer().analyze(symbol);
                } catch (Exception e) {
                    r = new StockResult();
                    r.symbol = symbol;
                    r.error = e.getMessage() == null ? "Errore dati" : e.getMessage();
                }

                synchronized (results) {
                    results.add(r);
                }

                synchronized (done) {
                    done[0]++;
                    int completed = done[0];
                    int pct = (int) Math.round(completed * 100.0 / total);
                    runOnUiThread(() -> {
                        progress.setProgress(pct);
                        status.setText("Analizzati " + completed + " di " + total);
                        if (completed == total) {
                            analyzeButton.setEnabled(true);
                            analyzeButton.setAlpha(1f);
                            progress.setVisibility(View.GONE);
                            sortResults();
                            showRanking();
                        }
                    });
                }
            });
        }
    }

    private void sortResults() {
        Collections.sort(results, (a, b) -> {
            boolean ao = Maths.ok(a.composite);
            boolean bo = Maths.ok(b.composite);
            if (ao && bo) return Double.compare(b.composite, a.composite);
            if (ao) return -1;
            if (bo) return 1;
            return a.symbol.compareTo(b.symbol);
        });
    }

    private void showRanking() {
        selectedResult = null;
        setScreen(SCREEN_RANKING, "Ranking", "Dal punteggio più alto al più basso", false);
        content.removeAllViews();

        if (results.isEmpty()) {
            emptyState(
                    "Nessun ranking disponibile",
                    "Avvia prima un'analisi dalla schermata Analizza.",
                    "Vai ad Analizza",
                    v -> showHome());
            return;
        }

        int valid = 0;
        for (StockResult r : results) if (Maths.ok(r.composite)) valid++;

        LinearLayout summary = card(SURFACE);
        summary.addView(text("PANORAMICA", 11, BLUE, true));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(10), 0, 0);
        StockResult leader = firstValid();

        stats.addView(statBlock("Titoli", String.valueOf(valid)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statBlock("Leader", leader == null ? "N/D" : leader.symbol),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statBlock("Score", leader == null ? "N/D" : fmtScore(leader.composite)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        summary.addView(stats);
        content.addView(summary, cardParams());

        TextView helper = text(
                "Tocca una scheda per aprire l'analisi del titolo.",
                12, MUTED, false);
        helper.setPadding(dp(2), dp(2), 0, dp(10));
        content.addView(helper);

        int rank = 1;
        for (StockResult r : results) {
            if (!Maths.ok(r.composite)) continue;
            content.addView(rankingCard(r, rank), cardParams());
            rank++;
        }

        int errors = 0;
        for (StockResult r : results) if (!Maths.ok(r.composite)) errors++;
        if (errors > 0) {
            LinearLayout errorCard = card(Color.rgb(42, 25, 27));
            errorCard.addView(text(errors + " ticker non elaborati", 14, Color.rgb(255, 178, 182), true));
            for (StockResult r : results) {
                if (Maths.ok(r.composite)) continue;
                errorCard.addView(text(
                        r.symbol + " · " + (r.error == null || r.error.isEmpty() ? "dati non disponibili" : r.error),
                        12, MUTED, false));
            }
            content.addView(errorCard, cardParams());
        }
    }

    private View rankingCard(StockResult r, int rank) {
        LinearLayout c = card(SURFACE);
        c.setClickable(true);
        c.setOnClickListener(v -> showDetail(r));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView rankBadge = text("#" + rank, 12, MUTED, true);
        rankBadge.setGravity(Gravity.CENTER);
        rankBadge.setBackground(rounded(SURFACE_2, 999, BORDER, 1));
        rankBadge.setPadding(dp(9), dp(5), dp(9), dp(5));
        top.addView(rankBadge);

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setPadding(dp(10), 0, 0, 0);
        identity.addView(text(r.symbol, 18, TEXT, true));
        identity.addView(text(
                r.name == null || r.name.isEmpty() ? r.symbol : r.name,
                12, MUTED, false));
        top.addView(identity, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView score = text(fmtScore(r.composite), 17, scoreColor(r.composite), true);
        score.setGravity(Gravity.CENTER);
        score.setBackground(scorePill(r.composite));
        score.setPadding(dp(10), dp(7), dp(10), dp(7));
        top.addView(score);

        c.addView(top);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setPadding(0, dp(12), 0, dp(8));
        meta.addView(text(fmtPrice(r), 13, TEXT, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        meta.addView(text(scoreBand(r.composite), 12, scoreColor(r.composite), true));
        c.addView(meta);

        c.addView(miniFactorLine("Quality", r.quality, "Momentum", r.momentum));
        c.addView(miniFactorLine("Value", r.valuation, "Risk", r.risk));

        TextView comment = text(r.comment, 12, MUTED, false);
        comment.setLineSpacing(dp(2), 1.0f);
        comment.setPadding(0, dp(10), 0, 0);
        c.addView(comment);

        TextView open = text("Apri dettaglio  ›", 12, BLUE, true);
        open.setGravity(Gravity.END);
        open.setPadding(0, dp(10), 0, 0);
        c.addView(open);
        return c;
    }

    private View miniFactorLine(String l1, double v1, String l2, double v2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));

        row.addView(miniMetric(l1, v1),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(miniMetric(l2, v2),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View miniMetric(String label, double value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = text(label, 11, MUTED, false);
        TextView v = text(Maths.ok(value) ? String.format(Locale.ITALY, "%.0f", value) : "N/D",
                11, Maths.ok(value) ? scoreColor(value) : MUTED, true);
        v.setPadding(dp(6), 0, 0, 0);
        box.addView(l);
        box.addView(v);
        return box;
    }

    private void showDetail(StockResult r) {
        selectedResult = r;
        setScreen(SCREEN_DETAIL, r.symbol, r.name == null ? "Dettaglio titolo" : r.name, true);
        content.removeAllViews();

        LinearLayout hero = card(SURFACE);
        LinearLayout priceLine = new LinearLayout(this);
        priceLine.setOrientation(LinearLayout.HORIZONTAL);
        priceLine.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout priceBox = new LinearLayout(this);
        priceBox.setOrientation(LinearLayout.VERTICAL);
        priceBox.addView(text("Prezzo", 11, MUTED, true));
        priceBox.addView(text(fmtPrice(r), 24, TEXT, true));
        priceLine.addView(priceBox, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout scoreBox = new LinearLayout(this);
        scoreBox.setOrientation(LinearLayout.VERTICAL);
        scoreBox.setGravity(Gravity.END);
        TextView scoreLabel = text("Composite", 11, MUTED, true);
        scoreLabel.setGravity(Gravity.END);
        TextView score = text(fmtScore(r.composite), 26, scoreColor(r.composite), true);
        score.setGravity(Gravity.END);
        scoreBox.addView(scoreLabel);
        scoreBox.addView(score);
        priceLine.addView(scoreBox);

        hero.addView(priceLine);

        TextView band = text(scoreBand(r.composite), 12, scoreColor(r.composite), true);
        band.setPadding(0, dp(10), 0, dp(4));
        hero.addView(band);

        TextView comment = text(r.comment, 13, TEXT, false);
        comment.setLineSpacing(dp(2), 1.0f);
        hero.addView(comment);
        content.addView(hero, cardParams());

        content.addView(sectionTitle("Profilo fattoriale", "Come si compone lo score"));

        LinearLayout factors = card(SURFACE);
        factors.addView(factorBar("Quality", r.quality));
        factors.addView(factorBar("Value", r.valuation));
        factors.addView(factorBar("Momentum", r.momentum));
        factors.addView(factorBar("Growth", r.growth));
        factors.addView(factorBar("Trend", r.trend));
        factors.addView(factorBar("Risk", r.risk));
        content.addView(factors, cardParams());

        content.addView(sectionTitle("Fondamentali", "Valutazione, redditività e crescita"));
        LinearLayout fundamentals = card(SURFACE);
        fundamentals.addView(metricLine("P/E", fmtNum(r.pe, "x")));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("FCF Yield", fmtPct(r.fcfYield)));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("ROE", fmtPct(r.roe)));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("Margine netto", fmtPct(r.profitMargin)));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("Leverage / Equity", fmtNum(r.debtToEquity, "")));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("Crescita ricavi", fmtPct(r.revenueGrowth)));
        fundamentals.addView(divider());
        fundamentals.addView(metricLine("Crescita utili", fmtPct(r.earningsGrowth)));
        content.addView(fundamentals, cardParams());

        content.addView(sectionTitle("Momentum e rischio", "Forza del prezzo e variabilità"));
        LinearLayout technical = card(SURFACE);
        technical.addView(metricLine("Momentum 3M", fmtPct(r.momentum3m)));
        technical.addView(divider());
        technical.addView(metricLine("Momentum 6M", fmtPct(r.momentum6m)));
        technical.addView(divider());
        technical.addView(metricLine("Momentum 12M", fmtPct(r.momentum12m)));
        technical.addView(divider());
        technical.addView(metricLine("Volatilità annua", fmtPct(r.volatilityAnnual)));
        technical.addView(divider());
        technical.addView(metricLine("Max drawdown", fmtPct(r.maxDrawdown)));
        technical.addView(divider());
        technical.addView(metricLine("RSI 14", fmtNum(r.rsi14, "")));
        technical.addView(divider());
        technical.addView(metricLine("ATR 14", fmtPct(r.atr14Pct)));
        content.addView(technical, cardParams());

        TextView backButton = secondaryButton("‹ Torna al ranking", v -> showRanking());
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(8);
        content.addView(backButton, bp);
    }

    private View factorBar(String label, double value) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(7), 0, dp(7));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(text(label, 13, TEXT, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(
                Maths.ok(value) ? String.format(Locale.ITALY, "%.0f/100", value) : "N/D",
                13, Maths.ok(value) ? scoreColor(value) : MUTED, true));
        wrap.addView(row);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Maths.ok(value) ? (int) Math.round(value) : 0);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                Maths.ok(value) ? scoreColor(value) : BORDER));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BORDER));
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(6);
        wrap.addView(bar, p);
        return wrap;
    }

    private void showInfo() {
        selectedResult = null;
        setScreen(SCREEN_INFO, "Info", "Metodo, dati e limiti", false);
        content.removeAllViews();

        LinearLayout intro = card(SURFACE);
        intro.addView(text("MARKET RANKER", 11, BLUE, true));
        TextView title = text("Cosa fa davvero l'app", 22, TEXT, true);
        title.setPadding(0, dp(6), 0, dp(8));
        intro.addView(title);
        intro.addView(text(
                "L'app è nativa Android: non apre Streamlit e non usa una WebView. " +
                "Recupera dati via HTTPS e calcola il ranking sul dispositivo.",
                14, MUTED, false));
        content.addView(intro, cardParams());

        content.addView(infoCard(
                "Fattori",
                "Quality 25% · Value 20% · Momentum 20% · Growth 15% · Trend 10% · Risk 10%. " +
                "Se un fattore manca, il composite ripesa automaticamente quelli disponibili."));

        content.addView(infoCard(
                "Fonti",
                "Prezzi e storico: Yahoo Finance chart endpoint. Fondamentali USA: SEC EDGAR. " +
                "Per i titoli non coperti da SEC, oggi il ranking si basa soprattutto sui fattori tecnici disponibili."));

        content.addView(infoCard(
                "Interpretazione",
                "70–100 indica un profilo forte secondo le soglie interne del modello; 50–69 intermedio; sotto 50 debole. " +
                "Non equivale a un segnale automatico di acquisto o vendita."));

        content.addView(infoCard(
                "Stato del modello",
                "Il motore è ancora un MVP di ricerca. Prima di usarlo operativamente, i pesi e le soglie devono essere validati con backtest."));
    }

    private View infoCard(String title, String body) {
        LinearLayout c = card(SURFACE);
        c.addView(text(title, 16, TEXT, true));
        TextView b = text(body, 13, MUTED, false);
        b.setLineSpacing(dp(2), 1.0f);
        b.setPadding(0, dp(6), 0, 0);
        c.addView(b);
        return c;
    }

    private View sectionTitle(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), dp(12), 0, dp(8));
        box.addView(text(title, 18, TEXT, true));
        box.addView(text(subtitle, 11, MUTED, false));
        return box;
    }

    private LinearLayout statBlock(String label, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label, 10, MUTED, true));
        TextView v = text(value, 18, TEXT, true);
        v.setPadding(0, dp(3), 0, 0);
        box.addView(v);
        return box;
    }

    private LinearLayout metricLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView l = text(label, 13, MUTED, false);
        TextView v = text(value, 14, TEXT, true);
        v.setGravity(Gravity.END);

        row.addView(l, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v);
        return row;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(BORDER);
        d.setAlpha(0.55f);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return d;
    }

    private LinearLayout card(int color) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(color, 18, BORDER, 1));
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        return c;
    }

    private TextView primaryButton(String label, View.OnClickListener listener) {
        TextView b = text(label, 15, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(BLUE_DARK, 14, BLUE, 1));
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        b.setOnClickListener(listener);
        return b;
    }

    private TextView secondaryButton(String label, View.OnClickListener listener) {
        TextView b = text(label, 14, BLUE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(SURFACE_2, 14, BORDER, 1));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setOnClickListener(listener);
        return b;
    }

    private void emptyState(String title, String body, String action, View.OnClickListener listener) {
        LinearLayout c = card(SURFACE);
        TextView icon = text("—", 34, BLUE, true);
        icon.setGravity(Gravity.CENTER);
        c.addView(icon);
        TextView t = text(title, 19, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(4), 0, dp(6));
        c.addView(t);
        TextView b = text(body, 13, MUTED, false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), dp(14));
        c.addView(b);
        c.addView(primaryButton(action, listener), buttonParams());
        content.addView(c, cardParams());
    }

    private StockResult firstValid() {
        for (StockResult r : results) if (Maths.ok(r.composite)) return r;
        return null;
    }

    private void navigateBack() {
        if (currentScreen == SCREEN_DETAIL) {
            showRanking();
        } else if (currentScreen == SCREEN_RANKING || currentScreen == SCREEN_INFO) {
            showHome();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == SCREEN_DETAIL) {
            showRanking();
            return;
        }
        if (currentScreen == SCREEN_RANKING || currentScreen == SCREEN_INFO) {
            showHome();
            return;
        }
        super.onBackPressed();
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0 && strokeColor != Color.TRANSPARENT) {
            d.setStroke(dp(strokeDp), strokeColor);
        }
        return d;
    }

    private GradientDrawable scorePill(double s) {
        int color = scoreColor(s);
        int fill;
        if (!Maths.ok(s)) fill = SURFACE_2;
        else if (s >= 70) fill = Color.rgb(19, 55, 36);
        else if (s >= 50) fill = Color.rgb(62, 44, 15);
        else fill = Color.rgb(62, 25, 30);
        return rounded(fill, 999, color, 1);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = dp(10);
        return p;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(10);
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int scoreColor(double s) {
        if (!Maths.ok(s)) return MUTED;
        if (s >= 70) return GREEN;
        if (s >= 50) return ORANGE;
        return RED;
    }

    private String scoreBand(double s) {
        if (!Maths.ok(s)) return "Dati insufficienti";
        if (s >= 70) return "Profilo forte";
        if (s >= 50) return "Profilo intermedio";
        return "Profilo debole";
    }

    private String fmtScore(double v) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%.0f/100", v) : "N/D";
    }

    private String fmtPrice(StockResult r) {
        return Maths.ok(r.price)
                ? String.format(Locale.ITALY, "%.2f %s", r.price,
                r.currency == null ? "" : r.currency)
                : "N/D";
    }

    private String fmtPct(double v) {
        return Maths.ok(v)
                ? String.format(Locale.ITALY, "%+.1f%%", v * 100.0)
                : "N/D";
    }

    private String fmtNum(double v, String suffix) {
        return Maths.ok(v)
                ? String.format(Locale.ITALY, "%.2f%s", v, suffix)
                : "N/D";
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
