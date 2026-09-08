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

        r.momentum = scale(Maths.mean(
                Maths.higher(r.momentum3m, -0.10, 0.20),
                Maths.higher(r.momentum6m, -0.15, 0.35),
                Maths.higher(r.momentum12m, -0.20, 0.50)));

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
            MarketDataClient.Fundamentals f = client.fundamentals(r.symbol);
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

            double marketCap = Maths.ok(f.marketCap) && f.marketCap > 0
                    ? f.marketCap
                    : ((Maths.ok(f.shares) && f.shares > 0) ? f.shares * r.price : Double.NaN);
            double fcf = Maths.ok(f.freeCashFlow)
                    ? f.freeCashFlow
                    : ((Maths.ok(f.operatingCashFlow) && Maths.ok(f.capex))
                        ? f.operatingCashFlow - Math.abs(f.capex) : Double.NaN);
            if (Maths.ok(marketCap) && marketCap > 0 && Maths.ok(fcf)) r.fcfYield = fcf / marketCap;
            if (Maths.ok(f.pe) && f.pe > 0) {
                r.pe = f.pe;
            } else if (Maths.ok(marketCap) && marketCap > 0 && Maths.ok(f.netIncome) && f.netIncome > 0) {
                r.pe = marketCap / f.netIncome;
            }

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

            r.fundamentalsAvailable = Maths.ok(r.quality) || Maths.ok(r.valuation) || Maths.ok(r.growth);
        } catch (Exception ignored) {
            r.fundamentalsAvailable = false;
        }

        r.composite = composite(r);
        buildScenario(r);
        buildSignals(r);
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

    private double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private void buildScenario(StockResult r) {
        if (!Maths.ok(r.price) || r.price <= 0) return;

        double momSignal = Maths.mean(r.momentum3m, r.momentum6m, r.momentum12m);
        if (!Maths.ok(momSignal)) momSignal = 0;
        double trendSignal = Maths.ok(r.trend) ? (r.trend - 50.0) / 50.0 : 0;
        double qualitySignal = Maths.ok(r.quality) ? (r.quality - 50.0) / 50.0 : 0;
        double growthSignal = Maths.ok(r.growth) ? (r.growth - 50.0) / 50.0 : 0;
        double valueSignal = Maths.ok(r.valuation) ? (r.valuation - 50.0) / 50.0 : 0;

        double statReturn =
                0.34 * nz(r.momentum6m) +
                0.22 * nz(r.momentum12m) +
                0.12 * nz(r.momentum3m) +
                0.055 * trendSignal +
                0.030 * qualitySignal +
                0.035 * growthSignal +
                0.025 * valueSignal;
        statReturn = clamp(statReturn, -0.32, 0.38);
        r.statisticalTarget = r.price * (1.0 + statReturn);

        if (Maths.ok(r.pe) && r.pe > 0) {
            double growth = Maths.ok(r.earningsGrowth) ? clamp(r.earningsGrowth, -0.45, 0.55) : 0;
            double currentEps = r.price / r.pe;
            double projectedEps = currentEps * Math.sqrt(Math.max(0.55, 1.0 + growth));

            double qualityAdj = Maths.ok(r.quality) ? (r.quality - 50.0) * 0.06 : 0;
            double growthAdj = Maths.ok(r.growth) ? (r.growth - 50.0) * 0.08 : 0;
            double normalizedPe = clamp(20.0 + qualityAdj + growthAdj, 10.0, 34.0);
            double peTarget = projectedEps * normalizedPe;

            if (Maths.ok(r.fcfYield) && r.fcfYield > -0.20) {
                double desiredYield = clamp(0.055 - 0.00020 * (Maths.ok(r.quality) ? r.quality - 50 : 0), 0.035, 0.080);
                double fcfTarget = r.fcfYield > 0 ? r.price * (r.fcfYield / desiredYield) : Double.NaN;
                r.fundamentalTarget = Maths.ok(fcfTarget)
                        ? 0.70 * peTarget + 0.30 * fcfTarget
                        : peTarget;
            } else {
                r.fundamentalTarget = peTarget;
            }
            r.fundamentalTarget = clamp(r.fundamentalTarget, r.price * 0.55, r.price * 1.65);
        }

        if (Maths.ok(r.fundamentalTarget)) {
            double factorWeight = 0.52;
            r.baseTarget = factorWeight * r.fundamentalTarget + (1.0 - factorWeight) * r.statisticalTarget;
            r.forecastMethod = "ibrido fondamentale + statistico";
        } else {
            r.baseTarget = r.statisticalTarget;
            r.forecastMethod = "statistico-tecnico";
        }

        double vol6m = Maths.ok(r.volatilityAnnual)
                ? clamp(r.volatilityAnnual * Math.sqrt(0.5), 0.10, 0.55)
                : 0.25;
        double downsideSpread = clamp(0.65 * vol6m + 0.05, 0.10, 0.42);
        double upsideSpread = clamp(0.55 * vol6m + 0.04, 0.09, 0.38);

        r.bearTarget = r.baseTarget * (1.0 - downsideSpread);
        r.bullTarget = r.baseTarget * (1.0 + upsideSpread);

        double direction = 0;
        if (Maths.ok(r.composite)) direction += (r.composite - 50.0) / 50.0 * 0.12;
        if (Maths.ok(r.momentum)) direction += (r.momentum - 50.0) / 50.0 * 0.09;
        if (Maths.ok(r.trend)) direction += (r.trend - 50.0) / 50.0 * 0.06;
        if (Maths.ok(r.risk)) direction += (r.risk - 50.0) / 50.0 * 0.04;
        direction = clamp(direction, -0.20, 0.20);

        r.bullProbability = clamp(0.20 + direction, 0.08, 0.38);
        r.bearProbability = clamp(0.20 - direction, 0.08, 0.38);
        r.baseProbability = 1.0 - r.bullProbability - r.bearProbability;

        r.expectedPrice6m =
                r.bearProbability * r.bearTarget +
                r.baseProbability * r.baseTarget +
                r.bullProbability * r.bullTarget;
        r.expectedReturn6m = r.expectedPrice6m / r.price - 1.0;
        r.downsideBear = r.bearTarget / r.price - 1.0;
        r.upsideBull = r.bullTarget / r.price - 1.0;

        double downsideAbs = Math.abs(Math.min(0, r.downsideBear));
        double upsideBase = Math.max(0, r.expectedReturn6m);
        r.riskReward = downsideAbs > 0.001 ? upsideBase / downsideAbs : Double.NaN;

        int coverage = 0;
        if (Maths.ok(r.momentum)) coverage++;
        if (Maths.ok(r.trend)) coverage++;
        if (Maths.ok(r.risk)) coverage++;
        if (Maths.ok(r.quality)) coverage++;
        if (Maths.ok(r.valuation)) coverage++;
        if (Maths.ok(r.growth)) coverage++;

        double coverageScore = coverage / 6.0;
        double agreementScore = 0.58;
        if (Maths.ok(r.fundamentalTarget) && Maths.ok(r.statisticalTarget)) {
            double gap = Math.abs(r.fundamentalTarget - r.statisticalTarget) / r.price;
            agreementScore = 1.0 - clamp(gap / 0.45, 0, 1);
        }
        double stabilityScore = Maths.ok(r.volatilityAnnual)
                ? 1.0 - clamp((r.volatilityAnnual - 0.18) / 0.55, 0, 1)
                : 0.45;
        r.confidence = clamp(
                100.0 * (0.48 * coverageScore + 0.30 * agreementScore + 0.22 * stabilityScore),
                20.0, 95.0);

        double returnScore = 100.0 * Maths.higher(r.expectedReturn6m, -0.08, 0.25);
        double downsideScore = 100.0 * Maths.lower(downsideAbs, 0.10, 0.38);
        double rrScore = Maths.ok(r.riskReward) ? 100.0 * Maths.higher(r.riskReward, 0.25, 1.50) : 45.0;
        double factorScore = Maths.ok(r.composite) ? r.composite : 50.0;

        r.opportunity =
                0.35 * returnScore +
                0.20 * downsideScore +
                0.15 * rrScore +
                0.15 * r.confidence +
                0.15 * factorScore;
        r.opportunity = clamp(r.opportunity, 0, 100);
    }

    private double nz(double x) {
        return Maths.ok(x) ? x : 0;
    }

    private void buildSignals(StockResult r) {
        if (Maths.ok(r.price) && r.price > 0 && Maths.ok(r.fundamentalTarget)) {
            r.valuationGap = r.fundamentalTarget / r.price - 1.0;
            if (r.valuationGap >= 0.10) r.valuationStatus = "UNDERVALUED";
            else if (r.valuationGap <= -0.10) r.valuationStatus = "OVERVALUED";
            else r.valuationStatus = "FAIR VALUE";
        } else if (Maths.ok(r.valuation)) {
            if (r.valuation >= 65) r.valuationStatus = "UNDERVALUED";
            else if (r.valuation <= 35) r.valuationStatus = "OVERVALUED";
            else r.valuationStatus = "FAIR VALUE";
        } else {
            r.valuationStatus = "N/D";
        }

        r.technicalSignalScore = Maths.mean(r.momentum, r.trend);
        if (Maths.ok(r.technicalSignalScore)) {
            if (r.technicalSignalScore >= 67) r.technicalSignal = "BULLISH";
            else if (r.technicalSignalScore <= 33) r.technicalSignal = "BEARISH";
            else r.technicalSignal = "NEUTRAL";
        } else {
            r.technicalSignal = "N/D";
        }

        if (Maths.ok(r.expectedReturn6m)) {
            if (r.expectedReturn6m >= 0.08) r.outlook6m = "BULLISH";
            else if (r.expectedReturn6m <= -0.08) r.outlook6m = "BEARISH";
            else r.outlook6m = "NEUTRAL";
        } else {
            r.outlook6m = "N/D";
        }

        int positive = 0;
        int negative = 0;
        if ("UNDERVALUED".equals(r.valuationStatus)) positive++;
        if ("OVERVALUED".equals(r.valuationStatus)) negative++;
        if ("BULLISH".equals(r.technicalSignal)) positive++;
        if ("BEARISH".equals(r.technicalSignal)) negative++;
        if ("BULLISH".equals(r.outlook6m)) positive++;
        if ("BEARISH".equals(r.outlook6m)) negative++;

        if (positive >= 3) r.overallVerdict = "STRONG POSITIVE";
        else if (negative >= 3) r.overallVerdict = "STRONG CAUTION";
        else if (positive >= 2 && negative == 0) r.overallVerdict = "POSITIVE";
        else if (negative >= 2 && positive == 0) r.overallVerdict = "CAUTION";
        else r.overallVerdict = "MIXED";
    }

    private String comment(StockResult r) {
        List<String> parts = new ArrayList<>();
        if (!"N/D".equals(r.valuationStatus)) parts.add("Valutazione: " + r.valuationStatus + ".");
        if (!"N/D".equals(r.technicalSignal)) parts.add("Segnale tecnico: " + r.technicalSignal + ".");
        if (!"N/D".equals(r.outlook6m)) parts.add("Outlook 6M: " + r.outlook6m + ".");

        if (Maths.ok(r.expectedReturn6m))
            parts.add(String.format("Scenario atteso 6M %+.1f%% con confidence %.0f/100.",
                    r.expectedReturn6m * 100, r.confidence));
        if (Maths.ok(r.momentum12m) && r.momentum >= 65)
            parts.add(String.format("Momentum %+.1f%% a 12 mesi.", r.momentum12m * 100));
        if (Maths.ok(r.pe) && r.pe > 30)
            parts.add(String.format("Valutazione impegnativa: P/E %.1fx.", r.pe));
        if (Maths.ok(r.volatilityAnnual) && r.volatilityAnnual > 0.40)
            parts.add(String.format("Volatilità elevata: %.1f%% annua.", r.volatilityAnnual * 100));

        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
    }
}
