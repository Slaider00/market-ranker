package com.slaider00.marketranker;

public class StockResult {
    public String symbol;
    public String name;
    public String currency;
    public double price = Double.NaN;

    public double composite = Double.NaN;
    public double quality = Double.NaN;
    public double valuation = Double.NaN;
    public double momentum = Double.NaN;
    public double growth = Double.NaN;
    public double trend = Double.NaN;
    public double risk = Double.NaN;

    public double pe = Double.NaN;
    public double fcfYield = Double.NaN;
    public double roe = Double.NaN;
    public double profitMargin = Double.NaN;
    public double debtToEquity = Double.NaN;
    public double revenueGrowth = Double.NaN;
    public double earningsGrowth = Double.NaN;

    public double momentum3m = Double.NaN;
    public double momentum6m = Double.NaN;
    public double momentum12m = Double.NaN;
    public double volatilityAnnual = Double.NaN;
    public double maxDrawdown = Double.NaN;
    public double rsi14 = Double.NaN;
    public double atr14Pct = Double.NaN;
    public double sma50 = Double.NaN;
    public double sma200 = Double.NaN;
    public double macd = Double.NaN;
    public double macdSignal = Double.NaN;

    public double bearTarget = Double.NaN;
    public double baseTarget = Double.NaN;
    public double bullTarget = Double.NaN;
    public double expectedPrice6m = Double.NaN;
    public double expectedReturn6m = Double.NaN;
    public double downsideBear = Double.NaN;
    public double upsideBull = Double.NaN;
    public double riskReward = Double.NaN;
    public double confidence = Double.NaN;
    public double opportunity = Double.NaN;
    public double bearProbability = Double.NaN;
    public double baseProbability = Double.NaN;
    public double bullProbability = Double.NaN;
    public double fundamentalTarget = Double.NaN;
    public double statisticalTarget = Double.NaN;
    public boolean fundamentalsAvailable = false;
    public String forecastMethod = "";

    public double valuationGap = Double.NaN;
    public double technicalSignalScore = Double.NaN;
    public String valuationStatus = "N/D";
    public String technicalSignal = "N/D";
    public String outlook6m = "N/D";
    public String overallVerdict = "N/D";

    public String comment = "";
    public String error = "";
}
