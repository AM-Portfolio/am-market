package com.am.marketdata.common.ipo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public final class IpoSubscription {
    private final String symbol;
    private final String series;
    private final OffsetDateTime updatedAt;
    private final Double overallTimes;
    private final Long overallSharesBid;
    private final Long overallSharesOffered;
    private final List<IpoSubscriptionCategory> categories;

    public IpoSubscription(
            String symbol,
            String series,
            OffsetDateTime updatedAt,
            Double overallTimes,
            Long overallSharesBid,
            Long overallSharesOffered,
            List<IpoSubscriptionCategory> categories) {
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.series = series;
        this.updatedAt = updatedAt;
        this.overallTimes = overallTimes;
        this.overallSharesBid = overallSharesBid;
        this.overallSharesOffered = overallSharesOffered;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSeries() {
        return series;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Double getOverallTimes() {
        return overallTimes;
    }

    public Long getOverallSharesBid() {
        return overallSharesBid;
    }

    public Long getOverallSharesOffered() {
        return overallSharesOffered;
    }

    public List<IpoSubscriptionCategory> getCategories() {
        return categories;
    }
}
