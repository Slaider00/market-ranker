package com.slaider00.marketranker;

import java.util.ArrayList;
import java.util.List;

public final class Maths {
    private Maths() {}

    public static boolean ok(double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x);
    }

    public static double clip01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    public static double higher(double x, double bad, double good) {
        if (!ok(x)) return Double.NaN;
        return clip01((x - bad) / (good - bad));
    }

    public static double lower(double x, double good, double bad) {
        if (!ok(x)) return Double.NaN;
        return clip01((bad - x) / (bad - good));
    }

    public static double mean(double... values) {
        double total = 0;
        int n = 0;
        for (double v : values) {
            if (ok(v)) {
                total += v;
                n++;
            }
        }
        return n == 0 ? Double.NaN : total / n;
    }

    public static double pctChange(List<Double> values, int tradingDaysBack) {
        if (values == null || values.size() < 2) return Double.NaN;
        int end = values.size() - 1;
        int start = Math.max(0, end - tradingDaysBack);
        double a = values.get(start);
        double b = values.get(end);
        if (a == 0 || !ok(a) || !ok(b)) return Double.NaN;
        return b / a - 1.0;
    }

    public static double sma(List<Double> values, int period) {
        if (values == null || values.size() < period) return Double.NaN;
        double total = 0;
        for (int i = values.size() - period; i < values.size(); i++) total += values.get(i);
        return total / period;
    }

    public static List<Double> emaSeries(List<Double> values, int period) {
        List<Double> out = new ArrayList<>();
        if (values == null || values.isEmpty()) return out;
        double alpha = 2.0 / (period + 1.0);
        double prev = values.get(0);
        out.add(prev);
        for (int i = 1; i < values.size(); i++) {
            prev = alpha * values.get(i) + (1 - alpha) * prev;
            out.add(prev);
        }
        return out;
    }

    public static double annualVolatility(List<Double> closes) {
        if (closes == null || closes.size() < 30) return Double.NaN;
        List<Double> rs = new ArrayList<>();
        for (int i = 1; i < closes.size(); i++) {
            double a = closes.get(i - 1), b = closes.get(i);
            if (a > 0 && b > 0) rs.add(Math.log(b / a));
        }
        if (rs.size() < 2) return Double.NaN;
        double mean = 0;
        for (double r : rs) mean += r;
        mean /= rs.size();
        double var = 0;
        for (double r : rs) var += (r - mean) * (r - mean);
        var /= (rs.size() - 1);
        return Math.sqrt(var) * Math.sqrt(252.0);
    }

    public static double maxDrawdown(List<Double> closes) {
        if (closes == null || closes.isEmpty()) return Double.NaN;
        double peak = closes.get(0);
        double worst = 0;
        for (double v : closes) {
            if (v > peak) peak = v;
            if (peak > 0) worst = Math.min(worst, v / peak - 1.0);
        }
        return worst;
    }

    public static double rsi(List<Double> closes, int period) {
        if (closes == null || closes.size() <= period) return Double.NaN;
        double gain = 0, loss = 0;
        int start = closes.size() - period;
        for (int i = start; i < closes.size(); i++) {
            double d = closes.get(i) - closes.get(i - 1);
            if (d > 0) gain += d;
            else loss -= d;
        }
        if (loss == 0) return 100.0;
        double rs = (gain / period) / (loss / period);
        return 100.0 - 100.0 / (1.0 + rs);
    }

    public static double atrPct(List<Double> high, List<Double> low, List<Double> close, int period) {
        if (close == null || close.size() <= period || high.size() != close.size() || low.size() != close.size()) return Double.NaN;
        double sum = 0;
        int start = close.size() - period;
        for (int i = start; i < close.size(); i++) {
            double prevClose = close.get(i - 1);
            double tr = Math.max(high.get(i) - low.get(i),
                    Math.max(Math.abs(high.get(i) - prevClose), Math.abs(low.get(i) - prevClose)));
            sum += tr;
        }
        double atr = sum / period;
        double p = close.get(close.size() - 1);
        return p == 0 ? Double.NaN : atr / p;
    }
}
