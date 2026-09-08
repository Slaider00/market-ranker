package com.slaider00.marketranker;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(14, 17, 23);
    private final int CARD = Color.rgb(26, 31, 40);
    private final int TEXT = Color.rgb(245, 247, 250);
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int BLUE = Color.rgb(47, 128, 237);
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int ORANGE = Color.rgb(245, 158, 11);
    private final int RED = Color.rgb(239, 68, 68);

    private LinearLayout root;
    private LinearLayout content;
    private EditText tickerInput;
    private Button analyzeButton;
    private ProgressBar progress;
    private TextView status;
    private final List<StockResult> results = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildShell();
        showHome();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(14), dp(14), dp(10));

        TextView kicker = text("EQUITY RESEARCH", 11, MUTED, true);
        root.addView(kicker);

        TextView title = text("Market Ranker", 28, TEXT, true);
        root.addView(title);

        TextView subtitle = text("Native Android · ranking multifattoriale · motore locale", 13, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, dp(12));
        root.addView(subtitle);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.addView(navButton("Ranking", v -> showRanking()), new LinearLayout.LayoutParams(0, dp(46), 1));
        nav.addView(navButton("Dettaglio", v -> showDetailChooser()), new LinearLayout.LayoutParams(0, dp(46), 1));
        nav.addView(navButton("Info", v -> showInfo()), new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(nav);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private void showHome() {
        content.removeAllViews();

        tickerInput = new EditText(this);
        tickerInput.setText("AAPL, MSFT, GOOGL, META, AMZN, NVDA, TTWO, ASML, SAP.DE, RHM.DE, BMPS.MI");
        tickerInput.setTextColor(TEXT);
        tickerInput.setHintTextColor(MUTED);
        tickerInput.setTextSize(15);
        tickerInput.setMinLines(3);
        tickerInput.setGravity(Gravity.TOP);
        tickerInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tickerInput.setBackgroundColor(CARD);
        tickerInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(tickerInput, matchWrap());

        analyzeButton = new Button(this);
        analyzeButton.setText("AGGIORNA RANKING");
        analyzeButton.setTextColor(Color.WHITE);
        analyzeButton.setBackgroundColor(BLUE);
        analyzeButton.setOnClickListener(v -> analyze());
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(10);
        content.addView(analyzeButton, bp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = matchWrap();
        pp.topMargin = dp(10);
        content.addView(progress, pp);

        status = text(results.isEmpty() ? "Inserisci i ticker e avvia l'analisi." :
                results.size() + " titoli già analizzati in questa sessione.", 13, MUTED, false);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(8);
        content.addView(status, sp);

        if (!results.isEmpty()) {
            Button open = new Button(this);
            open.setText("VEDI CLASSIFICA");
            open.setOnClickListener(v -> showRanking());
            content.addView(open, matchWrap());
        }
    }

    private void analyze() {
        String raw = tickerInput.getText().toString().replace("\n", ",");
        String[] parts = raw.split(",");
        List<String> symbols = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim().toUpperCase(Locale.ROOT);
            if (!s.isEmpty() && !symbols.contains(s)) symbols.add(s);
        }
        if (symbols.isEmpty()) return;

        results.clear();
        analyzeButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        status.setText("Analisi in corso…");

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
                    int pct = (int) Math.round(done[0] * 100.0 / total);
                    runOnUiThread(() -> {
                        progress.setProgress(pct);
                        status.setText("Analizzati " + done[0] + " / " + total);
                        if (done[0] == total) {
                            analyzeButton.setEnabled(true);
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
            boolean ao = Maths.ok(a.composite), bo = Maths.ok(b.composite);
            if (ao && bo) return Double.compare(b.composite, a.composite);
            if (ao) return -1;
            if (bo) return 1;
            return a.symbol.compareTo(b.symbol);
        });
    }

    private void showRanking() {
        content.removeAllViews();
        if (results.isEmpty()) {
            content.addView(text("Nessun ranking disponibile. Avvia prima l'analisi.", 15, MUTED, false));
            Button b = new Button(this);
            b.setText("TORNA ALL'ANALISI");
            b.setOnClickListener(v -> showHome());
            content.addView(b, matchWrap());
            return;
        }

        TextView h = text("Classifica", 22, TEXT, true);
        content.addView(h);

        TextView legend = text("70–100 forte · 50–69 intermedio · sotto 50 debole", 12, MUTED, false);
        legend.setPadding(0, dp(2), 0, dp(10));
        content.addView(legend);

        int rank = 1;
        for (StockResult r : results) {
            if (!Maths.ok(r.composite)) continue;
            LinearLayout card = card();
            card.setOnClickListener(v -> showDetail(r));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            TextView left = text("#" + rank + "  " + r.symbol, 17, TEXT, true);
            TextView score = text(fmtScore(r.composite), 18, scoreColor(r.composite), true);
            top.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            top.addView(score);
            card.addView(top);

            card.addView(text(r.name == null || r.name.isEmpty() ? r.symbol : r.name, 13, MUTED, false));
            card.addView(text(fmtPrice(r), 13, MUTED, false));

            TextView c = text(r.comment, 12, TEXT, false);
            c.setPadding(0, dp(8), 0, 0);
            card.addView(c);

            content.addView(card, cardParams());
            rank++;
        }

        Button edit = new Button(this);
        edit.setText("MODIFICA UNIVERSO");
        edit.setOnClickListener(v -> showHome());
        content.addView(edit, matchWrap());
    }

    private void showDetailChooser() {
        content.removeAllViews();
        if (results.isEmpty()) {
            content.addView(text("Calcola prima un ranking.", 15, MUTED, false));
            return;
        }
        content.addView(text("Seleziona titolo", 22, TEXT, true));
        for (StockResult r : results) {
            Button b = new Button(this);
            b.setText(r.symbol + (Maths.ok(r.composite) ? "   " + fmtScore(r.composite) : ""));
            b.setOnClickListener(v -> showDetail(r));
            content.addView(b, matchWrap());
        }
    }

    private void showDetail(StockResult r) {
        content.removeAllViews();
        content.addView(text(r.symbol, 26, TEXT, true));
        content.addView(text(r.name == null ? "" : r.name, 14, MUTED, false));

        LinearLayout summary = card();
        summary.addView(metricLine("Prezzo", fmtPrice(r)));
        summary.addView(metricLine("Composite", fmtScore(r.composite)));
        summary.addView(text(r.comment, 13, TEXT, false));
        content.addView(summary, cardParams());

        content.addView(section("Profilo fattoriale"));
        content.addView(factorRow("Quality", r.quality));
        content.addView(factorRow("Value", r.valuation));
        content.addView(factorRow("Momentum", r.momentum));
        content.addView(factorRow("Growth", r.growth));
        content.addView(factorRow("Trend", r.trend));
        content.addView(factorRow("Risk", r.risk));

        content.addView(section("Fondamentali"));
        content.addView(metricLine("P/E", fmtNum(r.pe, "x")));
        content.addView(metricLine("FCF Yield", fmtPct(r.fcfYield)));
        content.addView(metricLine("ROE", fmtPct(r.roe)));
        content.addView(metricLine("Margine netto", fmtPct(r.profitMargin)));
        content.addView(metricLine("Leverage / Equity", fmtNum(r.debtToEquity, "")));
        content.addView(metricLine("Crescita ricavi", fmtPct(r.revenueGrowth)));
        content.addView(metricLine("Crescita utili", fmtPct(r.earningsGrowth)));

        content.addView(section("Momentum e rischio"));
        content.addView(metricLine("Momentum 3M", fmtPct(r.momentum3m)));
        content.addView(metricLine("Momentum 6M", fmtPct(r.momentum6m)));
        content.addView(metricLine("Momentum 12M", fmtPct(r.momentum12m)));
        content.addView(metricLine("Volatilità annua", fmtPct(r.volatilityAnnual)));
        content.addView(metricLine("Max drawdown", fmtPct(r.maxDrawdown)));
        content.addView(metricLine("RSI 14", fmtNum(r.rsi14, "")));
        content.addView(metricLine("ATR 14", fmtPct(r.atr14Pct)));
    }

    private void showInfo() {
        content.removeAllViews();
        content.addView(text("Come funziona", 22, TEXT, true));
        LinearLayout c = card();
        c.addView(text(
                "Questa è un'app Android nativa: non incorpora il sito Streamlit e non usa una WebView. " +
                "Scarica i dati di mercato via HTTPS, calcola gli indicatori sul telefono e costruisce il ranking localmente.",
                14, TEXT, false));
        c.addView(text(
                "\nFattori: Quality 25%, Value 20%, Momentum 20%, Growth 15%, Trend 10%, Risk 10%. " +
                "Se un fattore non è disponibile, il composite ripesa automaticamente quelli presenti.",
                14, TEXT, false));
        c.addView(text(
                "\nPrezzi/storico: Yahoo Finance chart endpoint. Fondamentali USA: SEC EDGAR. " +
                "Per titoli non SEC il ranking usa i fattori tecnici disponibili.",
                14, TEXT, false));
        c.addView(text(
                "\nIl modello è ancora un MVP di ricerca e i pesi devono essere validati con backtest prima di uso operativo.",
                13, MUTED, false));
        content.addView(c, cardParams());
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(CARD);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        return c;
    }

    private LinearLayout metricLine(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView l = text(label, 14, MUTED, false);
        TextView v = text(value, 14, TEXT, true);
        v.setGravity(Gravity.END);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v);
        return row;
    }

    private LinearLayout factorRow(String label, double score) {
        LinearLayout row = metricLine(label, fmtScore(score));
        if (row.getChildCount() > 1 && row.getChildAt(1) instanceof TextView) {
            ((TextView) row.getChildAt(1)).setTextColor(scoreColor(score));
        }
        return row;
    }

    private TextView section(String s) {
        TextView t = text(s, 18, TEXT, true);
        t.setPadding(0, dp(18), 0, dp(4));
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button navButton(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setBackgroundColor(CARD);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = dp(8);
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

    private String fmtScore(double v) {
        return Maths.ok(v) ? String.format(Locale.ITALY, "%.0f/100", v) : "N/D";
    }

    private String fmtPrice(StockResult r) {
        return Maths.ok(r.price)
                ? String.format(Locale.ITALY, "%.2f %s", r.price, r.currency == null ? "" : r.currency)
                : "N/D";
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
