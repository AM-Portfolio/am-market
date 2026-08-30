package com.am.marketdata.provider.upstox.service;

import com.am.marketdata.common.model.fundamental.*;
import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Upstox implementation of {@link FundamentalDataProvider}.
 * Encapsulates Upstox Fundamentals REST API interactions, token retrieval from Redis,
 * JSON tree parsing, and fallback handling (e.g. standalone if consolidated is unavailable).
 */
@Slf4j
@Service("upstoxFundamentalProvider")
@RequiredArgsConstructor
public class UpstoxFundamentalProviderImpl implements FundamentalDataProvider {

    private static final String PROVIDER_NAME = "UPSTOX";
    private static final String BASE_URL = "https://api.upstox.com/v2/fundamentals";
    private static final String REDIS_KEY_ACCESS_TOKEN = "market_data:upstox:access_token";

    private final UpstoxConfig upstoxConfig;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Resolves active access token dynamically, prioritizing Redis session cache.
     */
    private String getAccessToken() {
        try {
            if (redisTemplate != null) {
                String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
                if (cachedToken != null && !cachedToken.trim().isEmpty()) {
                    return cachedToken.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve Upstox token from Redis: {}", e.getMessage());
        }
        return upstoxConfig != null ? upstoxConfig.getAccessToken() : null;
    }

    /**
     * Executes GET request to Upstox Fundamentals API with standard Bearer authorization.
     */
    private JsonNode executeGet(String endpointUrl) {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) {
            log.warn("Cannot execute Upstox API call, missing access token: {}", endpointUrl);
            return null;
        }

        try {
            HttpResponse<String> response = Unirest.get(endpointUrl)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .asString();

            if (response.getStatus() == 200 && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if ("success".equalsIgnoreCase(root.path("status").asText())) {
                    return root.path("data");
                }
            } else if (response.getStatus() == 401) {
                log.error("Upstox Fundamental API 401 Unauthorized for URL: {}", endpointUrl);
            } else {
                log.warn("Upstox Fundamental API returned HTTP {} for URL: {}", response.getStatus(), endpointUrl);
            }
        } catch (Exception e) {
            log.error("Error executing GET to Upstox Fundamentals URL {}: {}", endpointUrl, e.getMessage());
        }
        return null;
    }

    @Override
    public CompanyProfile getCompanyProfile(String isin) {
        String url = BASE_URL + "/" + isin + "/profile";
        JsonNode data = executeGet(url);
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }

        return CompanyProfile.builder()
                .description(data.path("company_profile").asText(null))
                .sector(data.path("sector").asText(null))
                .sectorMarketCapInr(data.hasNonNull("sector_market_cap_inr") ? data.path("sector_market_cap_inr").asDouble() : null)
                .sectorMarketCapUsd(data.hasNonNull("sector_market_cap_usd") ? data.path("sector_market_cap_usd").asDouble() : null)
                .build();
    }

    @Override
    public KeyRatios getKeyRatios(String isin) {
        String url = BASE_URL + "/" + isin + "/key-ratios";
        JsonNode data = executeGet(url);
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }

        KeyRatios.KeyRatiosBuilder builder = KeyRatios.builder();
        if (data.isArray()) {
            for (JsonNode item : data) {
                String ratioKey = item.path("key").asText("").toUpperCase();
                Double companyVal = item.hasNonNull("company_value") ? item.path("company_value").asDouble() : null;
                Double sectorVal = item.hasNonNull("sector_value") ? item.path("sector_value").asDouble() : null;

                switch (ratioKey) {
                    case "P/E", "PE" -> {
                        builder.pe(companyVal);
                        builder.sectorPe(sectorVal);
                    }
                    case "P/B", "PB" -> {
                        builder.pb(companyVal);
                        builder.sectorPb(sectorVal);
                    }
                    case "ROA" -> {
                        builder.roa(companyVal);
                        builder.sectorRoa(sectorVal);
                    }
                    case "ROE" -> {
                        builder.roe(companyVal);
                        builder.sectorRoe(sectorVal);
                    }
                    case "ROCE" -> {
                        builder.roce(companyVal);
                        builder.sectorRoce(sectorVal);
                    }
                    case "EV/EBITDA", "EV_EBITDA" -> {
                        builder.evEbitda(companyVal);
                        builder.sectorEvEbitda(sectorVal);
                    }
                    default -> log.debug("Unmapped key ratio from Upstox: {}", ratioKey);
                }
            }
        }
        return builder.build();
    }

    @Override
    public List<IncomeStatementEntry> getIncomeStatement(String isin, boolean consolidated, String timePeriod) {
        String type = consolidated ? "consolidated" : "standalone";
        String period = (timePeriod != null && !timePeriod.isEmpty()) ? timePeriod : "yearly";
        String url = BASE_URL + "/" + isin + "/income-statement?type=" + type + "&time_period=" + period + "&fs=true";

        JsonNode data = executeGet(url);
        // Fallback: If consolidated is missing or empty, attempt standalone
        if ((data == null || data.isEmpty()) && consolidated) {
            log.info("Consolidated Income Statement empty for isin={}, falling back to standalone", isin);
            return getIncomeStatement(isin, false, timePeriod);
        }

        if (data == null || data.isNull() || data.isMissingNode()) {
            return Collections.emptyList();
        }

        String unitsIn = data.path("units_in").asText("crore");
        String unit = "₹ " + unitsIn;

        Map<String, IncomeStatementEntry.IncomeStatementEntryBuilder> periodMap = new java.util.LinkedHashMap<>();

        // 1. Process full_statement particulars
        JsonNode fullStatement = data.path("full_statement");
        if (fullStatement.isArray()) {
            for (JsonNode row : fullStatement) {
                String particular = normalizeParticular(row.path("particular").asText(""));
                JsonNode history = row.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || pointPeriod.trim().isEmpty()) continue;

                        Double val = point.hasNonNull("value") ? point.path("value").asDouble() : null;
                        IncomeStatementEntry.IncomeStatementEntryBuilder builder = periodMap.computeIfAbsent(pointPeriod, p ->
                                IncomeStatementEntry.builder().period(p).type(type).timePeriod(period).unit(unit)
                        );

                        switch (particular) {
                            case "revenue" -> builder.revenue(val);
                            case "other income", "other_income" -> builder.otherIncome(val);
                            case "total revenue", "total_revenue" -> builder.totalRevenue(val);
                            case "total expenses", "total_expenses" -> builder.totalExpenses(val);
                            case "operating profit", "operating_profit" -> builder.operatingProfit(val);
                            case "profit before tax", "profit_before_tax" -> builder.profitBeforeTax(val);
                            case "tax" -> builder.tax(val);
                            case "profit after tax", "profit_after_tax", "net profit", "net_profit" -> builder.profitAfterTax(val);
                            case "eps - basic", "eps_basic", "basic eps" -> builder.epsBasic(val);
                            case "eps - diluted", "eps_diluted", "diluted eps" -> builder.epsDiluted(val);
                            default -> log.trace("Unmapped income statement particular: {}", particular);
                        }
                    }
                }
            }
        }

        // 2. Process growth metrics from income_statement summary array
        JsonNode summary = data.path("income_statement");
        if (summary.isArray()) {
            for (JsonNode cat : summary) {
                String category = normalizeParticular(cat.path("category").asText(""));
                JsonNode history = cat.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || !periodMap.containsKey(pointPeriod)) continue;

                        Double changeVal = parsePercentOrNull(point.path("change").asText(null));
                        IncomeStatementEntry.IncomeStatementEntryBuilder builder = periodMap.get(pointPeriod);
                        if (builder != null && changeVal != null) {
                            switch (category) {
                                case "revenue" -> builder.revenueChangePercent(changeVal);
                                case "operating profit", "operating_profit" -> builder.operatingProfitChangePercent(changeVal);
                                case "net profit", "net_profit" -> builder.netProfitChangePercent(changeVal);
                            }
                        }
                    }
                }
            }
        }

        List<IncomeStatementEntry> result = new ArrayList<>();
        periodMap.values().forEach(b -> result.add(b.build()));
        return result;
    }

    @Override
    public List<BalanceSheetEntry> getBalanceSheet(String isin, boolean consolidated) {
        String type = consolidated ? "consolidated" : "standalone";
        String url = BASE_URL + "/" + isin + "/balance-sheet?type=" + type + "&fs=true";

        JsonNode data = executeGet(url);
        if ((data == null || data.isEmpty()) && consolidated) {
            log.info("Consolidated Balance Sheet empty for isin={}, falling back to standalone", isin);
            return getBalanceSheet(isin, false);
        }

        if (data == null || data.isNull() || data.isMissingNode()) {
            return Collections.emptyList();
        }

        String unitsIn = data.path("units_in").asText("crore");
        String unit = "₹ " + unitsIn;

        Map<String, BalanceSheetEntry.BalanceSheetEntryBuilder> periodMap = new java.util.LinkedHashMap<>();

        // 1. Process full_statement particulars
        JsonNode fullStatement = data.path("full_statement");
        if (fullStatement.isArray()) {
            for (JsonNode row : fullStatement) {
                String particular = normalizeParticular(row.path("particular").asText(""));
                JsonNode history = row.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || pointPeriod.trim().isEmpty()) continue;

                        Double val = point.hasNonNull("value") ? point.path("value").asDouble() : null;
                        BalanceSheetEntry.BalanceSheetEntryBuilder builder = periodMap.computeIfAbsent(pointPeriod, p ->
                                BalanceSheetEntry.builder().period(p).type(type).unit(unit)
                        );

                        switch (particular) {
                            case "non-current assets", "non_current_assets" -> builder.nonCurrentAssets(val);
                            case "current assets", "current_assets" -> builder.currentAssets(val);
                            case "total assets", "total_assets" -> builder.totalAssets(val);
                            case "current liabilities", "current_liabilities" -> builder.currentLiabilities(val);
                            case "net current asset", "net current assets", "net_current_assets" -> builder.netCurrentAssets(val);
                            case "non-current liabilities", "non_current_liabilities" -> builder.nonCurrentLiabilities(val);
                            case "total liabilities", "total_liabilities" -> builder.totalLiabilities(val);
                            case "equity capital", "equity_capital", "shareholders funds" -> builder.equityCapital(val);
                            case "total equity & liabilities", "total equity and liabilities", "total_equity_and_liabilities" -> builder.totalEquityAndLiabilities(val);
                            default -> log.trace("Unmapped balance sheet particular: {}", particular);
                        }
                    }
                }
            }
        }

        // 2. Fallback / supplementary check from history summary
        JsonNode historySummary = data.path("history");
        if (historySummary.isArray()) {
            for (JsonNode point : historySummary) {
                String pointPeriod = point.path("period").asText(null);
                if (pointPeriod == null || pointPeriod.trim().isEmpty()) continue;

                BalanceSheetEntry.BalanceSheetEntryBuilder builder = periodMap.computeIfAbsent(pointPeriod, p ->
                        BalanceSheetEntry.builder().period(p).type(type).unit(unit)
                );
                if (point.hasNonNull("total_asset")) {
                    builder.totalAssets(point.path("total_asset").asDouble());
                }
                if (point.hasNonNull("total_liability")) {
                    builder.totalLiabilities(point.path("total_liability").asDouble());
                }
            }
        }

        List<BalanceSheetEntry> result = new ArrayList<>();
        periodMap.values().forEach(b -> result.add(b.build()));
        return result;
    }

    @Override
    public List<CashFlowEntry> getCashFlow(String isin, boolean consolidated) {
        String type = consolidated ? "consolidated" : "standalone";
        String url = BASE_URL + "/" + isin + "/cash-flow?type=" + type + "&fs=true";

        JsonNode data = executeGet(url);
        if ((data == null || data.isEmpty()) && consolidated) {
            log.info("Consolidated Cash Flow empty for isin={}, falling back to standalone", isin);
            return getCashFlow(isin, false);
        }

        if (data == null || data.isNull() || data.isMissingNode()) {
            return Collections.emptyList();
        }

        String unitsIn = data.path("units_in").asText("crore");
        String unit = "₹ " + unitsIn;

        Map<String, CashFlowEntry.CashFlowEntryBuilder> periodMap = new java.util.LinkedHashMap<>();

        // 1. Process full_statement particulars
        JsonNode fullStatement = data.path("full_statement");
        if (fullStatement.isArray()) {
            for (JsonNode row : fullStatement) {
                String particular = normalizeParticular(row.path("particular").asText(""));
                JsonNode history = row.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || pointPeriod.trim().isEmpty()) continue;

                        Double val = point.hasNonNull("value") ? point.path("value").asDouble() : null;
                        CashFlowEntry.CashFlowEntryBuilder builder = periodMap.computeIfAbsent(pointPeriod, p ->
                                CashFlowEntry.builder().period(p).type(type).unit(unit)
                        );

                        switch (particular) {
                            case "cash flow from operations", "cash flow from operating activities", "operating" -> builder.operatingCashFlow(val);
                            case "cash flow from investing", "cash flow from investing activities", "investing" -> builder.investingCashFlow(val);
                            case "cash flow from financing", "cash flow from financing activities", "financing" -> builder.financingCashFlow(val);
                            case "total cash flow", "net cash flow", "net change in cash" -> builder.netCashFlow(val);
                            default -> log.trace("Unmapped cash flow particular: {}", particular);
                        }
                    }
                }
            }
        }

        // 2. Process summary growth percentages
        JsonNode summary = data.path("cash_flow");
        if (summary.isArray()) {
            for (JsonNode cat : summary) {
                String category = normalizeParticular(cat.path("category").asText(""));
                JsonNode history = cat.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || !periodMap.containsKey(pointPeriod)) continue;

                        Double changeVal = parsePercentOrNull(point.path("change").asText(null));
                        CashFlowEntry.CashFlowEntryBuilder builder = periodMap.get(pointPeriod);
                        if (builder != null && changeVal != null) {
                            switch (category) {
                                case "operating" -> builder.operatingCashFlowChangePercent(changeVal);
                                case "investing" -> builder.investingCashFlowChangePercent(changeVal);
                                case "financing" -> builder.financingCashFlowChangePercent(changeVal);
                            }
                        }
                    }
                }
            }
        }

        List<CashFlowEntry> result = new ArrayList<>();
        periodMap.values().forEach(b -> result.add(b.build()));
        return result;
    }

    @Override
    public List<ShareholdingQuarterEntry> getShareHoldings(String isin) {
        String url = BASE_URL + "/" + isin + "/share-holdings";
        JsonNode data = executeGet(url);
        if (data == null || data.isNull() || data.isMissingNode()) {
            return Collections.emptyList();
        }

        Map<String, ShareholdingQuarterEntry.ShareholdingQuarterEntryBuilder> periodMap = new java.util.LinkedHashMap<>();

        if (data.isArray()) {
            for (JsonNode cat : data) {
                String category = normalizeParticular(cat.path("category").asText(""));
                JsonNode history = cat.path("history");
                if (history.isArray()) {
                    for (JsonNode point : history) {
                        String pointPeriod = point.path("period").asText(null);
                        if (pointPeriod == null || pointPeriod.trim().isEmpty()) continue;

                        Double val = point.hasNonNull("value") ? point.path("value").asDouble() : null;
                        ShareholdingQuarterEntry.ShareholdingQuarterEntryBuilder builder = periodMap.computeIfAbsent(pointPeriod, p ->
                                ShareholdingQuarterEntry.builder().period(p)
                        );

                        switch (category) {
                            case "promoters" -> builder.promotersPercent(val);
                            case "fii" -> builder.fiiPercent(val);
                            case "dii", "other_dii" -> builder.diiPercent(val);
                            case "mutual_funds", "mutual funds" -> builder.mutualFundsPercent(val);
                            case "retail_and_other", "retail and other", "public" -> builder.retailAndOtherPercent(val);
                        }
                    }
                }
            }
        }

        List<ShareholdingQuarterEntry> result = new ArrayList<>();
        periodMap.values().forEach(b -> result.add(b.build()));
        return result;
    }

    @Override
    public List<CorporateActionEntry> getCorporateActions(String isin) {
        String url = BASE_URL + "/" + isin + "/corporate-actions";
        JsonNode data = executeGet(url);
        if (data == null || !data.isArray()) {
            return Collections.emptyList();
        }

        List<CorporateActionEntry> list = new ArrayList<>();
        for (JsonNode node : data) {
            String type = node.path("name").asText(node.path("type").asText(null));
            Double amount = node.hasNonNull("amount") ? node.path("amount").asDouble() : null;
            String ratio = node.path("ratio").asText(null);
            String announcementDate = null;
            String exDate = null;
            String recordDate = null;
            String desc = null;

            JsonNode eventDetails = node.path("event_details");
            if (eventDetails.isArray()) {
                for (JsonNode item : eventDetails) {
                    String name = normalizeParticular(item.path("name").asText(""));
                    String val = item.path("value").asText(null);
                    switch (name) {
                        case "announcement date", "announcement_date" -> announcementDate = val;
                        case "ex dividend date", "ex date", "ex_date" -> exDate = val;
                        case "record date", "record_date" -> recordDate = val;
                        case "details" -> desc = val;
                    }
                }
            }

            list.add(CorporateActionEntry.builder()
                    .type(type)
                    .description(desc != null ? desc : node.path("description").asText(null))
                    .announcementDate(announcementDate != null ? announcementDate : node.path("announcement_date").asText(null))
                    .exDate(exDate != null ? exDate : node.path("expiry_date").asText(node.path("ex_date").asText(null)))
                    .recordDate(recordDate != null ? recordDate : node.path("record_date").asText(null))
                    .amount(amount)
                    .ratio(ratio)
                    .build());
        }
        return list;
    }

    @Override
    public List<CompetitorPeer> getCompetitors(String isin) {
        String instrumentKey = isin.startsWith("NSE_EQ|") || isin.startsWith("BSE_EQ|") ? isin : "NSE_EQ|" + isin;
        String encodedKey = instrumentKey.replace("|", "%7C");
        String url = BASE_URL + "/" + encodedKey + "/competitors";
        JsonNode data = executeGet(url);
        if (data == null || !data.isArray()) {
            return Collections.emptyList();
        }

        List<CompetitorPeer> list = new ArrayList<>();
        for (JsonNode node : data) {
            String key = node.path("instrument_key").asText(null);
            String peerIsin = null;
            if (key != null && key.contains("|")) {
                peerIsin = key.substring(key.indexOf("|") + 1);
            }

            list.add(CompetitorPeer.builder()
                    .instrumentKey(key)
                    .isin(peerIsin)
                    .companyName(node.path("company_profile").asText(null))
                    .sector(node.path("sector").asText(null))
                    .build());
        }
        return list;
    }

    private String normalizeParticular(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase();
    }

    private Double parsePercentOrNull(String percentStr) {
        if (percentStr == null || percentStr.trim().isEmpty()) return null;
        try {
            String cleaned = percentStr.replace("%", "").replace("+", "").trim();
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDoubleOrNull(JsonNode node, String fieldName) {
        if (node != null && node.hasNonNull(fieldName)) {
            return node.path(fieldName).asDouble();
        }
        return null;
    }
}
