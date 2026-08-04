package com.am.marketdata.common.ipo;

import java.time.LocalDate;
import java.util.Objects;

public final class IpoIssue {
    private final String symbol;
    private final String slug;
    private final String companyName;
    private final String series;
    private final IpoLifecycle lifecycle;
    private final IpoIssueStatus status;
    private final LocalDate openDate;
    private final LocalDate closeDate;
    private final LocalDate listingDate;
    private final LocalDate linkRemovalDate;
    private final String currency;
    private final Double priceMin;
    private final Double priceMax;
    private final Double issuePrice;
    private final String priceLabel;
    private final Long sharesOffered;
    private final String issueSizeLabel;
    private final boolean onNse;
    private final boolean onBse;
    private final Double subscriptionTimes;
    private final Long subscriptionSharesBid;
    private final Long subscriptionSharesOffered;

    public IpoIssue(
            String symbol,
            String slug,
            String companyName,
            String series,
            IpoLifecycle lifecycle,
            IpoIssueStatus status,
            LocalDate openDate,
            LocalDate closeDate,
            LocalDate listingDate,
            LocalDate linkRemovalDate,
            String currency,
            Double priceMin,
            Double priceMax,
            Double issuePrice,
            String priceLabel,
            Long sharesOffered,
            String issueSizeLabel,
            boolean onNse,
            boolean onBse,
            Double subscriptionTimes,
            Long subscriptionSharesBid,
            Long subscriptionSharesOffered) {
        this.symbol = Objects.requireNonNull(symbol, "symbol").trim().toUpperCase();
        this.slug = slug;
        this.companyName = companyName;
        this.series = series;
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.status = status == null ? IpoIssueStatus.UNKNOWN : status;
        this.openDate = Objects.requireNonNull(openDate, "openDate");
        this.closeDate = closeDate;
        this.listingDate = listingDate;
        this.linkRemovalDate = linkRemovalDate;
        this.currency = currency == null ? "INR" : currency;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.issuePrice = issuePrice;
        this.priceLabel = priceLabel;
        this.sharesOffered = sharesOffered;
        this.issueSizeLabel = issueSizeLabel;
        this.onNse = onNse;
        this.onBse = onBse;
        this.subscriptionTimes = subscriptionTimes;
        this.subscriptionSharesBid = subscriptionSharesBid;
        this.subscriptionSharesOffered = subscriptionSharesOffered;
    }

    public String id() {
        return symbol + ":" + openDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSlug() {
        return slug;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSeries() {
        return series;
    }

    public IpoLifecycle getLifecycle() {
        return lifecycle;
    }

    public IpoIssueStatus getStatus() {
        return status;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public LocalDate getListingDate() {
        return listingDate;
    }

    public LocalDate getLinkRemovalDate() {
        return linkRemovalDate;
    }

    public String getCurrency() {
        return currency;
    }

    public Double getPriceMin() {
        return priceMin;
    }

    public Double getPriceMax() {
        return priceMax;
    }

    public Double getIssuePrice() {
        return issuePrice;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public Long getSharesOffered() {
        return sharesOffered;
    }

    public String getIssueSizeLabel() {
        return issueSizeLabel;
    }

    public boolean isOnNse() {
        return onNse;
    }

    public boolean isOnBse() {
        return onBse;
    }

    public Double getSubscriptionTimes() {
        return subscriptionTimes;
    }

    public Long getSubscriptionSharesBid() {
        return subscriptionSharesBid;
    }

    public Long getSubscriptionSharesOffered() {
        return subscriptionSharesOffered;
    }
}
