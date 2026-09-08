package com.slaider00.marketranker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketDataClient {
    public static class PriceHistory {
        public String name = "";
        public String currency = "";
        public final List<Double> close = new ArrayList<>();
        public final List<Double> high = new ArrayList<>();
        public final List<Double> low = new ArrayList<>();
    }

    public static class Fundamentals {
        public double revenue = Double.NaN;
        public double prevRevenue = Double.NaN;
        public double netIncome = Double.NaN;
        public double prevNetIncome = Double.NaN;
        public double equity = Double.NaN;
        public double liabilities = Double.NaN;
        public double operatingCashFlow = Double.NaN;
        public double capex = Double.NaN;
        public double shares = Double.NaN;
    }

    private static Map<String, Integer> secTickerMap;

    private String get(String urlString, String userAgent) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept", "application/json");
        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status + ": " + sb);
        return sb.toString();
    }

    public PriceHistory yahooHistory(String symbol) throws Exception {
        String encoded = URLEncoder.encode(symbol, "UTF-8");
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + encoded +
                "?range=2y&interval=1d&includePrePost=false&events=div%2Csplits";
        JSONObject root = new JSONObject(get(url, "Mozilla/5.0 MarketRankerAndroid/1.0"));
        JSONObject chart = root.getJSONObject("chart");
        JSONArray results = chart.optJSONArray("result");
        if (results == null || results.length() == 0) throw new Exception("Nessun dato prezzo");
        JSONObject r = results.getJSONObject(0);

        PriceHistory out = new PriceHistory();
        JSONObject meta = r.optJSONObject("meta");
        if (meta != null) {
            out.name = meta.optString("longName", meta.optString("shortName", symbol));
            out.currency = meta.optString("currency", "");
        }

        JSONArray indicators = r.getJSONObject("indicators").getJSONArray("quote");
        JSONObject q = indicators.getJSONObject(0);
        JSONArray closes = q.getJSONArray("close");
        JSONArray highs = q.getJSONArray("high");
        JSONArray lows = q.getJSONArray("low");

        for (int i = 0; i < closes.length(); i++) {
            if (closes.isNull(i) || highs.isNull(i) || lows.isNull(i)) continue;
            double c = closes.optDouble(i, Double.NaN);
            double h = highs.optDouble(i, Double.NaN);
            double l = lows.optDouble(i, Double.NaN);
            if (Maths.ok(c) && Maths.ok(h) && Maths.ok(l)) {
                out.close.add(c);
                out.high.add(h);
                out.low.add(l);
            }
        }
        if (out.close.size() < 30) throw new Exception("Storico insufficiente");
        return out;
    }

    private synchronized void ensureSecTickerMap() throws Exception {
        if (secTickerMap != null) return;
        secTickerMap = new HashMap<>();
        JSONObject root = new JSONObject(get(
                "https://www.sec.gov/files/company_tickers.json",
                "MarketRankerAndroid/1.0 github.com/Slaider00/market-ranker"));
        java.util.Iterator<String> keys = root.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject row = root.getJSONObject(key);
            secTickerMap.put(row.getString("ticker").toUpperCase(), row.getInt("cik_str"));
        }
    }

    public Fundamentals secFundamentals(String symbol) throws Exception {
        if (symbol.contains(".")) throw new Exception("SEC non disponibile per questo mercato");
        ensureSecTickerMap();
        Integer cik = secTickerMap.get(symbol.toUpperCase());
        if (cik == null) throw new Exception("Ticker non presente in SEC");
        String cikText = String.format("%010d", cik);
        JSONObject root = new JSONObject(get(
                "https://data.sec.gov/api/xbrl/companyfacts/CIK" + cikText + ".json",
                "MarketRankerAndroid/1.0 github.com/Slaider00/market-ranker"));

        Fundamentals f = new Fundamentals();
        JSONObject facts = root.optJSONObject("facts");
        JSONObject usgaap = facts == null ? null : facts.optJSONObject("us-gaap");
        JSONObject dei = facts == null ? null : facts.optJSONObject("dei");

        f.revenue = latestAnnual(usgaap,
                "RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet");
        f.prevRevenue = previousAnnual(usgaap,
                "RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet");
        f.netIncome = latestAnnual(usgaap, "NetIncomeLoss", "ProfitLoss");
        f.prevNetIncome = previousAnnual(usgaap, "NetIncomeLoss", "ProfitLoss");
        f.equity = latestInstant(usgaap,
                "StockholdersEquity", "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest");
        f.liabilities = latestInstant(usgaap, "Liabilities");
        f.operatingCashFlow = latestAnnual(usgaap, "NetCashProvidedByUsedInOperatingActivities");
        f.capex = latestAnnual(usgaap, "PaymentsToAcquirePropertyPlantAndEquipment");
        f.shares = latestInstant(dei, "EntityCommonStockSharesOutstanding");
        return f;
    }

    private double latestAnnual(JSONObject taxonomy, String... concepts) {
        List<Double> vals = annualValues(taxonomy, concepts);
        return vals.isEmpty() ? Double.NaN : vals.get(vals.size() - 1);
    }

    private double previousAnnual(JSONObject taxonomy, String... concepts) {
        List<Double> vals = annualValues(taxonomy, concepts);
        return vals.size() < 2 ? Double.NaN : vals.get(vals.size() - 2);
    }

    private List<Double> annualValues(JSONObject taxonomy, String... concepts) {
        List<Double> out = new ArrayList<>();
        if (taxonomy == null) return out;
        for (String concept : concepts) {
            JSONObject node = taxonomy.optJSONObject(concept);
            if (node == null) continue;
            JSONObject units = node.optJSONObject("units");
            if (units == null) continue;
            JSONArray arr = units.optJSONArray("USD");
            if (arr == null) continue;

            List<JSONObject> rows = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject x = arr.optJSONObject(i);
                if (x == null) continue;
                String form = x.optString("form");
                String fp = x.optString("fp");
                if (("10-K".equals(form) || "10-K/A".equals(form)) && "FY".equals(fp) && x.has("val")) rows.add(x);
            }
            rows.sort((a, b) -> {
                String ae = a.optString("end") + a.optString("filed");
                String be = b.optString("end") + b.optString("filed");
                return ae.compareTo(be);
            });

            String lastEnd = "";
            for (JSONObject x : rows) {
                String end = x.optString("end");
                if (!end.equals(lastEnd)) {
                    out.add(x.optDouble("val", Double.NaN));
                    lastEnd = end;
                } else if (!out.isEmpty()) {
                    out.set(out.size() - 1, x.optDouble("val", Double.NaN));
                }
            }
            if (!out.isEmpty()) return out;
        }
        return out;
    }

    private double latestInstant(JSONObject taxonomy, String... concepts) {
        if (taxonomy == null) return Double.NaN;
        for (String concept : concepts) {
            JSONObject node = taxonomy.optJSONObject(concept);
            if (node == null) continue;
            JSONObject units = node.optJSONObject("units");
            if (units == null) continue;
            JSONArray arr = units.optJSONArray("USD");
            if (arr == null) arr = units.optJSONArray("shares");
            if (arr == null) continue;

            JSONObject best = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject x = arr.optJSONObject(i);
                if (x == null || !x.has("val")) continue;
                String form = x.optString("form");
                if (!("10-K".equals(form) || "10-Q".equals(form) || "10-K/A".equals(form) || "10-Q/A".equals(form))) continue;
                if (best == null) best = x;
                else {
                    String a = best.optString("end") + best.optString("filed");
                    String b = x.optString("end") + x.optString("filed");
                    if (b.compareTo(a) > 0) best = x;
                }
            }
            if (best != null) return best.optDouble("val", Double.NaN);
        }
        return Double.NaN;
    }
}
