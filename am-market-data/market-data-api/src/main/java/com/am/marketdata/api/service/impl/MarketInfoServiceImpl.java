package com.am.marketdata.api.service.impl;

import com.am.marketdata.api.service.MarketInfoService;
import com.am.marketdata.common.model.marketinfo.ChangeOiData;
import com.am.marketdata.common.model.marketinfo.ChangeOiResponse;
import com.am.marketdata.common.model.marketinfo.FlowsOverviewResponse;
import com.am.marketdata.common.model.marketinfo.InstitutionalFlowResponse;
import com.am.marketdata.common.model.marketinfo.OiData;
import com.am.marketdata.common.model.marketinfo.OiResponse;
import com.am.marketdata.common.model.marketinfo.OiSummary;
import com.am.marketdata.common.model.marketinfo.UpstoxInstrumentKeys;
import com.am.marketdata.provider.upstox.client.UpStockClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class MarketInfoServiceImpl implements MarketInfoService {
    private static final List<String> DEFAULT_FII_TYPES = List.of(
            "NSE_FO|INDEX_FUTURES",
            "NSE_FO|STOCK_FUTURES",
            "NSE_FO|INDEX_OPTIONS",
            "NSE_FO|STOCK_OPTIONS",
            "NSE_EQ|CASH");
    private static final List<String> OVERVIEW_FII_TYPES =
            List.of("NSE_FO|INDEX_FUTURES", "NSE_FO|INDEX_OPTIONS", "NSE_EQ|CASH");
    private static final String DEFAULT_EXPIRY = "current_month";
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final UpStockClient upStockClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public InstitutionalFlowResponse getFii(List<String> dataTypes, String interval, String from) {
        List<String> types = normalizeTypes(dataTypes);
        String normalizedInterval = normalizeInterval(interval);
        String key = "market_info:fii:" + normalizedInterval + ":" + String.join(",", types);
        InstitutionalFlowResponse response = cached(key, ttlSeconds(normalizedInterval),
                InstitutionalFlowResponse.class,
                () -> upStockClient.getFiiRaw(types, normalizedInterval, from));
        return normalizeFlows(response);
    }

    @Override
    public InstitutionalFlowResponse getDii(String interval, String from) {
        String normalizedInterval = normalizeInterval(interval);
        String key = "market_info:dii:" + normalizedInterval;
        InstitutionalFlowResponse response = cached(key, ttlSeconds(normalizedInterval),
                InstitutionalFlowResponse.class,
                () -> upStockClient.getDiiRaw(normalizedInterval, from));
        return normalizeFlows(response);
    }

    @Override
    public OiResponse getOi(String symbol, String expiry, String date) {
        String instrumentKey = UpstoxInstrumentKeys.mapSymbolToInstrumentKey(symbol);
        String normalizedExpiry = defaultIfBlank(expiry, DEFAULT_EXPIRY);
        String normalizedDate = defaultIfBlank(date, today());
        String key = "market_info:oi:" + instrumentKey + ":" + normalizedExpiry + ":" + normalizedDate;
        OiResponse response = cached(key, TimeUnit.MINUTES.toSeconds(30), OiResponse.class,
                () -> upStockClient.getOiRaw(instrumentKey, normalizedExpiry, normalizedDate));
        return normalizeOi(response);
    }

    @Override
    public ChangeOiResponse getChangeOi(
            String symbol, String expiry, String date, int intervalDays) {
        String instrumentKey = UpstoxInstrumentKeys.mapSymbolToInstrumentKey(symbol);
        String normalizedExpiry = defaultIfBlank(expiry, DEFAULT_EXPIRY);
        String normalizedDate = defaultIfBlank(date, today());
        int normalizedInterval = Math.max(intervalDays, 1);
        String key = "market_info:change-oi:" + instrumentKey + ":" + normalizedExpiry
                + ":" + normalizedDate + ":" + normalizedInterval;
        ChangeOiResponse response = cached(key, TimeUnit.MINUTES.toSeconds(30), ChangeOiResponse.class,
                () -> upStockClient.getChangeOiRaw(
                        instrumentKey, normalizedExpiry, normalizedDate, normalizedInterval));
        return normalizeChangeOi(response);
    }

    @Override
    public FlowsOverviewResponse getFlowsOverview(String interval) {
        String normalizedInterval = normalizeInterval(interval);
        InstitutionalFlowResponse fii = getFii(OVERVIEW_FII_TYPES, normalizedInterval, null);
        InstitutionalFlowResponse dii = getDii(normalizedInterval, null);
        List<OiSummary> summaries = new ArrayList<>();
        for (String symbol : List.of("NIFTY 50", "NIFTY BANK")) {
            OiResponse response = getOi(symbol, DEFAULT_EXPIRY, today());
            OiData data = response.getData();
            summaries.add(OiSummary.builder()
                    .symbol(symbol)
                    .instrumentKey(UpstoxInstrumentKeys.mapSymbolToInstrumentKey(symbol))
                    .totalCalls(data.getTotalCalls())
                    .totalPuts(data.getTotalPuts())
                    .spot(data.getSpotClosingPrice())
                    .expiry(data.getExpiry())
                    .pcr(calculatePcr(data.getTotalPuts(), data.getTotalCalls()))
                    .build());
        }
        return FlowsOverviewResponse.builder()
                .interval(normalizedInterval)
                .fii(fii)
                .dii(dii)
                .oi(summaries)
                .build();
    }

    private <T> T cached(String key, long ttlSeconds, Class<T> type, Supplier<String> loader) {
        String json = null;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ignored) {
            // Redis is an optimization; Upstox remains the source of truth.
        }
        try {
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, type);
            }
            T value = objectMapper.readValue(loader.get(), type);
            try {
                redisTemplate.opsForValue().set(
                        key, objectMapper.writeValueAsString(value), ttlSeconds, TimeUnit.SECONDS);
            } catch (RuntimeException ignored) {
                // Return live data if the cache is unavailable.
            }
            return value;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to parse Upstox " + type.getSimpleName(), e);
        }
    }

    private InstitutionalFlowResponse normalizeFlows(InstitutionalFlowResponse response) {
        if (response == null) {
            return new InstitutionalFlowResponse("success", Collections.emptyMap());
        }
        if (response.getData() == null) {
            response.setData(Collections.emptyMap());
        }
        return response;
    }

    private OiResponse normalizeOi(OiResponse response) {
        if (response == null) {
            response = new OiResponse("success", null);
        }
        if (response.getData() == null) {
            response.setData(OiData.builder().callPutOiDataList(Collections.emptyList()).build());
        } else if (response.getData().getCallPutOiDataList() == null) {
            response.getData().setCallPutOiDataList(Collections.emptyList());
        }
        return response;
    }

    private ChangeOiResponse normalizeChangeOi(ChangeOiResponse response) {
        if (response == null) {
            response = new ChangeOiResponse("success", null);
        }
        if (response.getData() == null) {
            response.setData(ChangeOiData.builder().callPutOiDataList(Collections.emptyList()).build());
        } else if (response.getData().getCallPutOiDataList() == null) {
            response.getData().setCallPutOiDataList(Collections.emptyList());
        }
        return response;
    }

    private List<String> normalizeTypes(List<String> dataTypes) {
        if (dataTypes == null || dataTypes.isEmpty()) {
            return DEFAULT_FII_TYPES;
        }
        List<String> normalized = dataTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(this::normalizeDataType)
                .distinct()
                .sorted()
                .toList();
        return normalized.isEmpty() ? DEFAULT_FII_TYPES : normalized;
    }

    private String normalizeDataType(String dataType) {
        if (dataType.contains("|")) {
            return dataType;
        }
        return "CASH".equals(dataType) ? "NSE_EQ|CASH" : "NSE_FO|" + dataType;
    }

    private long ttlSeconds(String interval) {
        return "1M".equalsIgnoreCase(interval)
                ? TimeUnit.HOURS.toSeconds(6)
                : TimeUnit.MINUTES.toSeconds(30);
    }

    private String normalizeInterval(String interval) {
        return defaultIfBlank(interval, "1D").toUpperCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String today() {
        return LocalDate.now(INDIA_ZONE).toString();
    }

    private Double calculatePcr(Long puts, Long calls) {
        return calls == null || calls == 0 || puts == null ? null : puts.doubleValue() / calls;
    }
}
