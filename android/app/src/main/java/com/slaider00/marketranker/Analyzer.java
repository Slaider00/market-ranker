package com.slaider00.marketranker;

import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    private final MarketDataClient client = new MarketDataClient();

    public StockResult analyze(String symbol) throws Exception {
        StockResult r = new StockResult();
        r.symbol = symbol.trim().toUpperCase();

        MarketDataClient.PriceHistory h = client.yahooHistory(r.symbol);
        r.name = h.name == null || h.name.isEmpty() ? r.symbol : h.name;
        r.currency = h.currency;
        r.price = h.close.get(h.close.size() - 1);

        r.momentum3m = Maths.pctChange(h.close, 63);
        r.momentum6m = Maths.pctChange(h.close, 126);
        r.momentum12m = Maths.pctChange(h.close, 252);
        r.volatilityAnnual = Maths.annualVolatility(h.close);
        r.maxDrawdown = Maths.maxDrawdown(h.close);
        r.rsi14 = Maths.rsi(h.close, 14);
        r.atr14Pct = Maths.atrPct(h.high, h.low, h.close, 14);
        r.sma50 = Maths.sma(h.close, 50);
        r.sma200 = Maths.sma(h.close, 200);

        List<Double> ema12 = Maths.emaSeries(h.close, 12);
        List<Double> ema26 = Maths.emaSeries(h.close, 26);
        List<Double> macdSeries = new ArrayList<>();
        for (int i = 0; i < h.close.size(); i++) macdSeries.add(ema12.get(i) - ema26.get(i));
        List<Double> signal = Maths.emaSeries(macdSeries, 9);
        r.macd = macdSeries.get(macdSeries.size() - 1);
        r.macdSignal = signal.get(signal.size() - 1);

        double m = Maths.mean(
                Maths.higher(r.momentum3m, -0.10, 0.20),
                Maths.higher(r.momentum6m, -0.15, 0.35),
                Maths.higher(r.momentum12m, -0.20, 0.50));
        r.momentum = scale(m);

        double p50 = Maths.ok(r.sma50) ? r.price / r.sma50 - 1 : Double.NaN;
        double p200 = Maths.ok(r.sma200) ? r.price / r.sma200 - 1 : Double.NaN;
        double macdNorm = r.price == 0 ? Double.NaN : (r.macd - r.macdSignal) / r.price;
        r.trend = scale(Maths.mean(
                Maths.higher(p50, -0.10, 0.15),
                Maths.higher(p200, -0.15, 0.25),
                Maths.higher(macdNorm, -0.01, 0.01)));

        r.risk = scale(Maths.mean(
                Maths.lower(r.volatilityAnnual, 0.15, 0.60),
                Maths.lower(Math.abs(r.maxDrawdown), 0.15, 0.55),
                Maths.lower(r.atr14Pct, 0.015, 0.06)));

        try {
            MarketDataClient.Fundamentals f = client.secFundamentals(r.symbol);
            if (Maths.ok(f.revenue) && f.revenue != 0 && Maths.ok(f.netIncome))
                r.profitMargin = f.netIncome / f.revenue;
            if (Maths.ok(f.equity) && f.equity != 0 && Maths.ok(f.netIncome))
                r.roe = f.netIncome / f.equity;
            if (Maths.ok(f.equity) && f.equity != 0 && Maths.ok(f.liabilities))
                r.debtToEquity = f.liabilities / f.equity * 100.0;
            if (Maths.ok(f.revenue) && Maths.ok(f.prevRevenue) && f.prevRevenue != 0)
                r.revenueGrowth = f.revenue / f.prevRevenue - 1.0;
            if (Maths.ok(f.netIncome) && Maths.ok(f.prevNetIncome) && f.prevNetIncome != 0)
                r.earningsGrowth = f.netIncome / f.prevNetIncome - 1.0;

            double marketCap = (Maths.ok(f.shares) && f.shares > 0) ? f.shares * r.price : Double.NaN;
            double fcf = (Maths.ok(f.operatingCashFlow) && Maths.ok(f.capex))
                    ? f.operatingCashFlow - Math.abs(f.capex) : Double.NaN;
            if (Maths.ok(marketCap) && marketCap > 0 && Maths.ok(fcf)) r.fcfYield = fcf / marketCap;
            if (Maths.ok(marketCap) && marketCap > 0 && Maths.ok(f.netIncome) && f.netIncome > 0)
                r.pe = marketCap / f.netIncome;

            double cashConversion = (Maths.ok(f.operatingCashFlow) && Maths.ok(f.netIncome) && f.netIncome > 0)
                    ? f.operatingCashFlow / f.netIncome : Double.NaN;

            r.quality = scale(Maths.mean(
                    Maths.higher(r.roe, 0.05, 0.25),
                    Maths.higher(r.profitMargin, 0.03, 0.20),
                    Maths.lower(r.debtToEquity, 30, 180),
                    Maths.higher(cashConversion, 0.7, 1.3)));

            r.valuation = scale(Maths.mean(
                    Maths.lower(r.pe, 12, 35),
                    Maths.higher(r.fcfYield, 0.02, 0.08)));

            r.growth = scale(Maths.mean(
                    Maths.higher(r.revenueGrowth, 0.00, 0.20),
                    Maths.higher(r.earningsGrowth, 0.00, 0.25)));
        } catch (Exception ignored) {
            // Non-US or unavailable fundamentals: technical factors remain valid.
        }

        r.composite = composite(r);
        r.comment = comment(r);
        return r;
    }

    private double scale(double x) {
        return Maths.ok(x) ? x * 100.0 : Double.NaN;
    }

    private double composite(StockResult r) {
        double total = 0, weights = 0;
        total += add(r.valuation, 0.20); if (Maths.ok(r.valuation)) weights += 0.20;
        total += add(r.quality, 0.25); if (Maths.ok(r.quality)) weights += 0.25;
        total += add(r.growth, 0.15); if (Maths.ok(r.growth)) weights += 0.15;
        total += add(r.momentum, 0.20); if (Maths.ok(r.momentum)) weights += 0.20;
        total += add(r.trend, 0.10); if (Maths.ok(r.trend)) weights += 0.10;
        total += add(r.risk, 0.10); if (Maths.ok(r.risk)) weights += 0.10;
        return weights == 0 ? Double.NaN : total / weights;
    }

    private double add(double value, double weight) {
        return Maths.ok(value) ? value * weight : 0;
    }

    private String comment(StockResult r) {
        List<String> parts = new ArrayList<>();
        if (r.composite >= 70) parts.add("Profilo complessivamente forte.");
        else if (r.composite >= 50) parts.add("Profilo complessivamente intermedio.");
        else parts.add("Profilo complessivamente debole.");

        if (Maths.ok(r.momentum12m) && r.momentum >= 65)
            parts.add(String.format("Momentum sostenuto dal %+.1f%% a 12 mesi.", r.momentum12m * 100));
        if (Maths.ok(r.roe) && r.quality >= 65)
            parts.add(String.format("Quality sostenuta da ROE %.1f%%.", r.roe * 100));
        if (Maths.ok(r.pe) && r.pe > 30)
            parts.add(String.format("Value penalizzato da P/E %.1fx.", r.pe));
        if (Maths.ok(r.volatilityAnnual) && r.volatilityAnnual > 0.40)
            parts.add(String.format("Rischio penalizzato da volatilità %.1f%%.", r.volatilityAnnual * 100));
        if (parts.size() == 1)
            parts.add("Il punteggio combina i fattori disponibili e ripesa automaticamente quelli mancanti.");

        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
    }
}
