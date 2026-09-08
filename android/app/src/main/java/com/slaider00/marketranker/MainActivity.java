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

    private final int BG = Color.rgb(9, 13, 19);
    private final int SURFACE = Color.rgb(18, 24, 33);
    private final int SURFACE_2 = Color.rgb(24, 31, 42);
    private final int BORDER = Color.rgb(42, 53, 68);
    private final int TEXT = Color.rgb(244, 247, 251);
    private final int MUTED = Color.rgb(151, 163, 181);
    private final int BLUE = Color.rgb(67, 139, 250);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(245, 158, 11);
    private final int RED = Color.rgb(239, 68, 68);
    private final int PURPLE = Color.rgb(167, 139, 250);

    private LinearLayout content;
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
    private boolean sortOpportunity = false;
    private String lastUniverse = "";

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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildTopBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(buildBottomNav());
        setContentView(root);
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(14), dp(10));
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
        topSubtitle = text("Equity research · native Android", 11, MUTED, false);
        titles.addView(topTitle);
        titles.addView(topSubtitle);
        bar.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView chip = text("6M ENGINE", 9, PURPLE, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(9), dp(6), dp(9), dp(6));
        chip.setBackground(rounded(Color.rgb(45, 32, 69), 999, Color.rgb(91, 67, 137), 1));
        bar.addView(chip);
        return bar;
    }

    private View buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(10), dp(8), dp(10), dp(10));
        nav.setBackgroundColor(SURFACE);

        navHome = navItem("⌂\nAnalizza", v -> showHome());
        navRanking = navItem("≡\nRanking", v -> showRanking());
        navInfo = navItem("i\nInfo", v -> showInfo());

        nav.addView(navHome, new LinearLayout.LayoutParams(0, dp(58), 1));
        nav.addView(navRanking, new LinearLayout.LayoutParams(0, dp(58), 1));
        nav.addView(navInfo, new LinearLayout.LayoutParams(0, dp(58), 1));
        return nav;
    }

    private TextView navItem(String label, View.OnClickListener listener) {
        TextView t = text(label, 12, MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(listener);
        return t;
    }

    private void setScreen(int screen, String title, String subtitle, boolean back) {
        currentScreen = screen;
        topTitle.setText(title);
        topSubtitle.setText(subtitle);
        topBack.setVisibility(back ? View.VISIBLE : View.GONE);

        styleNav(navHome, screen == SCREEN_HOME);
        styleNav(navRanking, screen == SCREEN_RANKING);
        styleNav(navInfo, screen == SCREEN_INFO);
    }

    private void styleNav(TextView item, boolean active) {
        item.setTextColor(active ? BLUE : MUTED);
        item.setBackground(active ? rounded(Color.rgb(18, 41, 69), 14, Color.TRANSPARENT, 0) : null);
    }

    private void showHome() {
        setScreen(SCREEN_HOME, "Market Ranker", "Screening + scenario engine 6M", false);
        content.removeAllViews();

        LinearLayout hero = card(SURFACE);
        hero.addView(text("MARKET RANKER", 11, BLUE, true));
        TextView title = text("Trova il titolo. Poi valuta l'opportunità.", 24, TEXT, true);
        title.setPadding(0, dp(6), 0, dp(8));
        hero.addView(title);
        hero.addView(text(
                "Il Factor Score ordina qualità, valore, crescita, momentum, trend e rischio. " +
                "Il 6M Engine costruisce Bear, Base e Bull senza modificare il ranking originale.",
                14, MUTED, false));
        content.addView(hero, cardParams());

        TextView label = text("Inserisci i titoli", 13, TEXT, true);
        label.setPadding(dp(2), dp(8), 0, dp(8));
        content.addView(label);

        tickerInput = new EditText(this);
        tickerInput.setText(lastUniverse);
        tickerInput.setHint("Es. AAPL, META, TTWO oppure BMPS.MI");
        tickerInput.setTextColor(TEXT);
        tickerInput.setHintTextColor(MUTED);
        tickerInput.setTextSize(15);
        tickerInput.setMinLines(4);
        tickerInput.setGravity(Gravity.TOP | Gravity.START);
        tickerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tickerInput.setBackground(rounded(SURFACE, 16, BORDER, 1));
        tickerInput.setPadding(dp(14), dp(13), dp(14), dp(13));
        content.addView(tickerInput, matchWrap());

        analyzeButton = primaryButton("Analizza", v -> analyze());
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
                results.isEmpty() ? "Inserisci uno o più ticker e premi Analizza." :
                        results.size() + " titoli disponibili nella sessione.",
                12, MUTED, false);
        status.setPadding(dp(2), dp(10), 0, 0);
        content.addView(status);

        if (!results.isEmpty()) {
            LinearLayout recent = card(SURFACE_2);
            StockResult leader = leader();
            recent.addView(text("ULTIMA ANALISI", 11, MUTED, true));
            if (leader != null) {
                recent.addView(metricLine("Leader Factor", leader.symbol + " · " + fmtScore(leader.composite)));
                recent.addView(metricLine("Opportunity", fmtScore(leader.opportunity)));
                recent.addView(metricLine("Atteso 6M", fmtPct(leader.expectedReturn6m)));
            }
            recent.addView(secondaryButton("Apri ranking", v -> showRanking()), buttonParams());
            content.addView(recent, cardParams());
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
        analyzeButton.setAlpha(.55f);
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
                            sortFactor();
                            showRanking();
                        }
                    });
                }
            });
        }
    }

    private void sortFactor() {
        Collections.sort(results, (a, b) -> compareScore(b.composite, a.composite));
    }

    private void sortOpportunity() {
        Collections.sort(results, (a, b) -> compareScore(b.opportunity, a.opportunity));
    }

    private int compareScore(double a, double b) {
        boolean ao = Maths.ok(a), bo = Maths.ok(b);
        if (ao && bo) return Double.compare(a, b);
        if (ao) return -1;
        if (bo) return 1;
        return 0;
    }

    private void showRanking() {
        setScreen(SCREEN_RANKING, "Ranking", sortOpportunity ? "Opportunity Score" : "Factor Score", false);
        content.removeAllViews();

        if (results.isEmpty()) {
            emptyState("Nessun ranking", "Inserisci alcuni ticker dalla schermata Analizza.", "Vai ad Analizza", v -> showHome());
            return;
        }

        LinearLayout sorter = new LinearLayout(this);
        sorter.setOrientation(LinearLayout.HORIZONTAL);
        TextView factor = sortButton("Factor", !sortOpportunity, v -> {
            sortOpportunity = false;
            sortFactor();
            showRanking();
        });
        TextView opp = sortButton("Opportunity", sortOpportunity, v -> {
            sortOpportunity = true;
            sortOpportunity();
            showRanking();
        });
        sorter.addView(factor, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, dp(42), 1);
        op.leftMargin = dp(8);
        sorter.addView(opp, op);
        content.addView(sorter, cardParams());

        StockResult lead = firstValid();
        if (lead != null) {
            LinearLayout summary = card(SURFACE);
            summary.addView(text("LEADER", 11, BLUE, true));
            summary.addView(text(lead.symbol + " · " + safeName(lead), 20, TEXT, true));
            summary.addView(metricLine("Factor", fmtScore(lead.composite)));
            summary.addView(metricLine("Opportunity", fmtScore(lead.opportunity)));
            summary.addView(metricLine("Atteso 6M", fmtPct(lead.expectedReturn6m)));
            summary.addView(metricLine("Confidence", fmtScore(lead.confidence)));
            content.addView(summary, cardParams());
        }

        int rank = 1;
        for (StockResult r : results) {
            double primary = sortOpportunity ? r.opportunity : r.composite;
            if (!Maths.ok(primary)) continue;
            content.addView(rankingCard(r, rank), cardParams());
            rank++;
        }

        int errors = 0;
        for (StockResult r : results) if (!Maths.ok(r.composite)) errors++;
        if (errors > 0) {
            LinearLayout e = card(Color.rgb(45, 24, 28));
            e.addView(text(errors + " ticker non elaborati", 14, Color.rgb(255, 183, 188), true));
            for (StockResult r : results) {
                if (Maths.ok(r.composite)) continue;
                e.addView(text(r.symbol + " · " + (r.error == null ? "errore" : r.error), 12, MUTED, false));
            }
            content.addView(e, cardParams());
        }
    }

    private View rankingCard(StockResult r, int rank) {
        LinearLayout c = card(SURFACE);
        c.setClickable(true);
        c.setOnClickListener(v -> showDetail(r));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = text("#" + rank, 12, MUTED, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(rounded(SURFACE_2, 999, BORDER, 1));
        top.addView(badge);

        LinearLayout id = new LinearLayout(this);
        id.setOrientation(LinearLayout.VERTICAL);
        id.setPadding(dp(10), 0, 0, 0);
        id.addView(text(r.symbol, 18, TEXT, true));
        id.addView(text(safeName(r), 12, MUTED, false));
        top.addView(id, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        double primary = sortOpportunity ? r.opportunity : r.composite;
        TextView score = text(fmtScore(primary), 16, scoreColor(primary), true);
        score.setPadding(dp(10), dp(7), dp(10), dp(7));
        score.setBackground(scorePill(primary));
        top.addView(score);
        c.addView(top);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(12), 0, 0);
        metrics.addView(statBlock("Factor", fmtScore(r.composite)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        metrics.addView(statBlock("Opp.", fmtScore(r.opportunity)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        metrics.addView(statBlock("6M", fmtPct(r.expectedReturn6m)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        c.addView(metrics);

        LinearLayout signals = new LinearLayout(this);
        signals.setOrientation(LinearLayout.HORIZONTAL);
        signals.setPadding(0, dp(10), 0, 0);
        signals.addView(signalBlock("Valutazione", r.valuationStatus),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams ts = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        ts.leftMargin = dp(6); ts.rightMargin = dp(6);
        signals.addView(signalBlock("Tecnico", r.technicalSignal), ts);
        signals.addView(signalBlock("Outlook 6M", r.outlook6m),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        c.addView(signals);

        TextView comment = text(r.comment, 12, MUTED, false);
        comment.setPadding(0, dp(10), 0, 0);
        c.addView(comment);
        return c;
    }

    private void showDetail(StockResult r) {
        setScreen(SCREEN_DETAIL, r.symbol, "Analisi completa", true);
        content.removeAllViews();

        LinearLayout hero = card(SURFACE);
        hero.addView(text(safeName(r), 13, MUTED, false));
        TextView px = text(fmtPrice(r), 27, TEXT, true);
        px.setPadding(0, dp(5), 0, dp(10));
        hero.addView(px);

        LinearLayout scores = new LinearLayout(this);
        scores.setOrientation(LinearLayout.HORIZONTAL);
        scores.addView(statBlock("Factor", fmtScore(r.composite)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        scores.addView(statBlock("Opportunity", fmtScore(r.opportunity)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        scores.addView(statBlock("Confidence", fmtScore(r.confidence)),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(scores);

        TextView comment = text(r.comment, 13, TEXT, false);
        comment.setPadding(0, dp(12), 0, 0);
        hero.addView(comment);
        content.addView(hero, cardParams());

        LinearLayout verdict = card(SURFACE_2);
        verdict.addView(text("VERDETTO RAPIDO", 11, PURPLE, true));
        TextView verdictText = text(r.overallVerdict, 22, statusColor(r.overallVerdict), true);
        verdictText.setPadding(0, dp(6), 0, dp(10));
        verdict.addView(verdictText);
        verdict.addView(signalLine("Valutazione", r.valuationStatus));
        verdict.addView(signalLine("Segnale tecnico", r.technicalSignal));
        verdict.addView(signalLine("Outlook 6 mesi", r.outlook6m));
        if (Maths.ok(r.valuationGap)) {
            verdict.addView(metricLine("Scarto fair value", fmtPct(r.valuationGap)));
        }
        content.addView(verdict, cardParams());

        content.addView(section("Scenario 6 mesi"));
        LinearLayout scenarios = new LinearLayout(this);
        scenarios.setOrientation(LinearLayout.HORIZONTAL);
        scenarios.addView(scenarioCard("BEAR", r.bearTarget, r.bearProbability, RED),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        b.leftMargin = dp(7); b.rightMargin = dp(7);
        scenarios.addView(scenarioCard("BASE", r.baseTarget, r.baseProbability, BLUE), b);
        scenarios.addView(scenarioCard("BULL", r.bullTarget, r.bullProbability, GREEN),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        content.addView(scenarios, cardParams());

        LinearLayout expected = card(SURFACE_2);
        expected.addView(text("EXPECTED PRICE 6M", 11, PURPLE, true));
        expected.addView(metricLine("Prezzo atteso", fmtMoney(r.expectedPrice6m, r.currency)));
        expected.addView(metricLine("Rendimento atteso", fmtPct(r.expectedReturn6m)));
        expected.addView(metricLine("Bear downside", fmtPct(r.downsideBear)));
        expected.addView(metricLine("Bull upside", fmtPct(r.upsideBull)));
        expected.addView(metricLine("Risk / Reward", Maths.ok(r.riskReward) ? String.format(Locale.ITALY, "%.2f", r.riskReward) : "N/D"));
        expected.addView(metricLine("Metodo", r.forecastMethod == null ? "N/D" : r.forecastMethod));
        content.addView(expected, cardParams());

        content.addView(section("Factor profile"));
        content.addView(factorRow("Quality", r.quality, "redditività, margini, leva e cassa"));
        content.addView(factorRow("Value", r.valuation, "P/E e FCF Yield"));
        content.addView(factorRow("Momentum", r.momentum, "forza a 3, 6 e 12 mesi"));
        content.addView(factorRow("Growth", r.growth, "crescita ricavi e utili"));
        content.addView(factorRow("Trend", r.trend, "medie mobili e MACD"));
        content.addView(factorRow("Risk", r.risk, "volatilità, drawdown e ATR"));

        content.addView(section("Fondamentali"));
        if (!r.fundamentalsAvailable) {
            LinearLayout warn = card(Color.rgb(52, 42, 18));
            warn.addView(text("Fondamentali incompleti", 14, ORANGE, true));
            warn.addView(text(
                    "Per questo ticker il motore non ha ottenuto un set fondamentale completo SEC. " +
                    "Il forecast 6M è quindi prevalentemente statistico-tecnico.",
                    12, MUTED, false));
            content.addView(warn, cardParams());
        }
        content.addView(metricLine("P/E", fmtNum(r.pe, "x")));
        content.addView(metricLine("FCF Yield", fmtPct(r.fcfYield)));
        content.addView(metricLine("ROE", fmtPct(r.roe)));
        content.addView(metricLine("Margine netto", fmtPct(r.profitMargin)));
        content.addView(metricLine("Debt / Equity", fmtNum(r.debtToEquity, "")));
        content.addView(metricLine("Crescita ricavi", fmtPct(r.revenueGrowth)));
        content.addView(metricLine("Crescita utili", fmtPct(r.earningsGrowth)));

        content.addView(section("Tecnica e rischio"));
        content.addView(metricLine("Momentum 3M", fmtPct(r.momentum3m)));
        content.addView(metricLine("Momentum 6M", fmtPct(r.momentum6m)));
        content.addView(metricLine("Momentum 12M", fmtPct(r.momentum12m)));
        content.addView(metricLine("RSI 14", fmtNum(r.rsi14, "")));
        content.addView(metricLine("Volatilità annua", fmtPct(r.volatilityAnnual)));
        content.addView(metricLine("Max drawdown", fmtPct(r.maxDrawdown)));
        content.addView(metricLine("ATR 14", fmtPct(r.atr14Pct)));

        content.addView(section("Motori del forecast"));
        content.addView(metricLine("Target statistico", fmtMoney(r.statisticalTarget, r.currency)));
        content.addView(metricLine("Target fondamentale", fmtMoney(r.fundamentalTarget, r.currency)));
        content.addView(text(
                "Il target a 6 mesi è una stima modellistica, non una previsione certa né una raccomandazione. " +
                "Il Confidence Score misura copertura dati, accordo tra modelli e stabilità del titolo.",
                11, MUTED, false));
    }

    private View scenarioCard(String label, double target, double probability, int color) {
        LinearLayout c = card(SURFACE);
        c.setPadding(dp(10), dp(10), dp(10), dp(10));
        TextView l = text(label, 10, color, true);
        l.setGravity(Gravity.CENTER);
        c.addView(l);
        TextView p = text(Maths.ok(target) ? String.format(Locale.ITALY, "%.2f", target) : "N/D", 16, TEXT, true);
        p.setGravity(Gravity.CENTER);
        p.setPadding(0, dp(5), 0, dp(4));
        c.addView(p);
        TextView prob = text(Maths.ok(probability) ? String.format(Locale.ITALY, "%.0f%%", probability * 100) : "N/D", 11, MUTED, false);
        prob.setGravity(Gravity.CENTER);
        c.addView(prob);
        return c;
    }

    private View factorRow(String label, double score, String description) {
        LinearLayout c = card(SURFACE);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(label, 14, TEXT, true));
        left.addView(text(description, 11, MUTED, false));
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView s = text(fmtScore(score), 14, scoreColor(score), true);
        s.setPadding(dp(9), dp(6), dp(9), dp(6));
        s.setBackground(scorePill(score));
        row.addView(s);
        c.addView(row);
        return c;
    }

    private void showInfo() {
        setScreen(SCREEN_INFO, "Metodologia", "Come leggere il modello", false);
        content.removeAllViews();

        LinearLayout a = card(SURFACE);
        a.addView(text("DUE SCORE, DUE DOMANDE", 11, BLUE, true));
        a.addView(text("Factor Score", 18, TEXT, true));
        a.addView(text("Quali titoli hanno il profilo multifattoriale più interessante?", 13, MUTED, false));
        TextView o = text("Opportunity Score", 18, TEXT, true);
        o.setPadding(0, dp(14), 0, 0);
        a.addView(o);
        a.addView(text("Quanto è interessante oggi il rapporto fra upside, downside, confidence e qualità del profilo?", 13, MUTED, false));
        content.addView(a, cardParams());

        LinearLayout b = card(SURFACE);
        b.addView(text("SCENARIO ENGINE 6M", 11, PURPLE, true));
        b.addView(text(
                "Il modello combina target statistico-tecnico e, quando disponibili, fondamentali SEC. " +
                "Da qui costruisce Bear, Base, Bull e probabilità dinamiche.",
                13, TEXT, false));
        b.addView(text(
                "\nLa Confidence aumenta con copertura dati, accordo fra motori e minore instabilità. " +
                "Per titoli non coperti da SEC il forecast resta soprattutto statistico-tecnico.",
                12, MUTED, false));
        content.addView(b, cardParams());

        LinearLayout c = card(SURFACE_2);
        c.addView(text("PESI FACTOR SCORE", 11, MUTED, true));
        c.addView(metricLine("Quality", "25%"));
        c.addView(metricLine("Value", "20%"));
        c.addView(metricLine("Momentum", "20%"));
        c.addView(metricLine("Growth", "15%"));
        c.addView(metricLine("Trend", "10%"));
        c.addView(metricLine("Risk", "10%"));
        content.addView(c, cardParams());

        content.addView(text(
                "Il modello è ancora da validare tramite backtest point-in-time. Gli scenari servono per ricerca comparativa, non garantiscono rendimenti futuri.",
                11, MUTED, false));
    }

    private View signalBlock(String label, String status) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(6), dp(8), dp(6), dp(8));
        b.setBackground(rounded(SURFACE_2, 12, BORDER, 1));
        TextView l = text(label, 9, MUTED, true);
        l.setGravity(Gravity.CENTER);
        b.addView(l);
        TextView s = text(status == null ? "N/D" : status, 11, statusColor(status), true);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, dp(4), 0, 0);
        b.addView(s);
        return b;
    }

    private View signalLine(String label, String status) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView l = text(label, 13, MUTED, false);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView s = text(status == null ? "N/D" : status, 13, statusColor(status), true);
        s.setPadding(dp(10), dp(6), dp(10), dp(6));
        s.setBackground(statusPill(status));
        row.addView(s);
        return row;
    }

    private int statusColor(String status) {
        if (status == null) return MUTED;
        if (status.contains("UNDERVALUED") || status.contains("BULLISH") || status.contains("POSITIVE")) return GREEN;
        if (status.contains("OVERVALUED") || status.contains("BEARISH") || status.contains("CAUTION")) return RED;
        if (status.contains("FAIR") || status.contains("NEUTRAL") || status.contains("MIXED")) return ORANGE;
        return MUTED;
    }

    private GradientDrawable statusPill(String status) {
        int c = statusColor(status);
        if (c == GREEN) return rounded(Color.rgb(19, 55, 36), 999, GREEN, 1);
        if (c == RED) return rounded(Color.rgb(62, 25, 30), 999, RED, 1);
        if (c == ORANGE) return rounded(Color.rgb(62, 44, 15), 999, ORANGE, 1);
        return rounded(SURFACE_2, 999, BORDER, 1);
    }

    private TextView sortButton(String label, boolean active, View.OnClickListener l) {
        TextView t = text(label, 13, active ? TEXT : MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(active ? Color.rgb(29, 64, 111) : SURFACE, 12, active ? BLUE : BORDER, 1));
        t.setOnClickListener(l);
        return t;
    }

    private TextView primaryButton(String label, View.OnClickListener l) {
        TextView t = text(label, 15, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(BLUE, 14, BLUE, 1));
        t.setPadding(dp(12), dp(14), dp(12), dp(14));
        t.setOnClickListener(l);
        return t;
    }

    private TextView secondaryButton(String label, View.OnClickListener l) {
        TextView t = text(label, 14, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(SURFACE_2, 12, BORDER, 1));
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setOnClickListener(l);
        return t;
    }

    private LinearLayout card(int color) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(13), dp(14), dp(13));
        c.setBackground(rounded(color, 16, BORDER, 1));
        return c;
    }

    private LinearLayout metricLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView l = text(label, 13, MUTED, false);
        TextView v = text(value, 13, TEXT, true);
        v.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v);
        return row;
    }

    private View statBlock(String label, String value) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        TextView l = text(label, 10, MUTED, true);
        l.setGravity(Gravity.CENTER);
        TextView v = text(value, 14, TEXT, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(3), 0, 0);
        b.addView(l);
        b.addView(v);
        return b;
    }

    private TextView section(String title) {
        TextView t = text(title, 18, TEXT, true);
        t.setPadding(dp(2), dp(14), 0, dp(8));
        return t;
    }

    private void emptyState(String title, String body, String action, View.OnClickListener l) {
        LinearLayout c = card(SURFACE);
        TextView a = text(title, 19, TEXT, true);
        a.setGravity(Gravity.CENTER);
        c.addView(a);
        TextView b = text(body, 13, MUTED, false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(8), dp(12), dp(14));
        c.addView(b);
        c.addView(primaryButton(action, l), buttonParams());
        content.addView(c, cardParams());
    }

    private StockResult leader() {
        if (results.isEmpty()) return null;
        List<StockResult> copy = new ArrayList<>(results);
        Collections.sort(copy, (a, b) -> compareScore(b.composite, a.composite));
        for (StockResult r : copy) if (Maths.ok(r.composite)) return r;
        return null;
    }

    private StockResult firstValid() {
        for (StockResult r : results) {
            double v = sortOpportunity ? r.opportunity : r.composite;
            if (Maths.ok(v)) return r;
        }
        return null;
    }

    private String safeName(StockResult r) {
        return r.name == null || r.name.isEmpty() ? r.symbol : r.name;
    }

    private void navigateBack() {
        if (currentScreen == SCREEN_DETAIL) showRanking();
        else if (currentScreen == SCREEN_RANKING || currentScreen == SCREEN_INFO) showHome();
        else finish();
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
        if (strokeDp > 0 && strokeColor != Color.TRANSPARENT) d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    private GradientDrawable scorePill(double s) {
        if (!Maths.ok(s)) return rounded(SURFACE_2, 999, BORDER, 1);
        if (s >= 70) return rounded(Color.rgb(19, 55, 36), 999, GREEN, 1);
        if (s >= 50) return rounded(Color.rgb(62, 44, 15), 999, ORANGE, 1);
        return rounded(Color.rgb(62, 25, 30), 999, RED, 1);
    }

    private int scoreColor(double s) {
        if (!Maths.ok(s)) return MUTED;
        if (s >= 70) return GREEN;
        if (s >= 50) return ORANGE;
        return RED;
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

    private String fmtScore(double v) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%.0f/100", v) : "N/D";
    }

    private String fmtPrice(StockResult r) {
        return Maths.ok(r.price) ? fmtMoney(r.price, r.currency) : "N/D";
    }

    private String fmtMoney(double v, String currency) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%.2f %s", v, currency == null ? "" : currency) : "N/D";
    }

    private String fmtPct(double v) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%+.1f%%", v * 100.0) : "N/D";
    }

    private String fmtNum(double v, String suffix) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%.2f%s", v, suffix) : "N/D";
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
