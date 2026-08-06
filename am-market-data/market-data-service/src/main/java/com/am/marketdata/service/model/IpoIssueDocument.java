package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ipo_issues")
public class IpoIssueDocument {

    @Id
    private String id;

    @Indexed
    private String symbol;

    private String slug;

    @Field("company_name")
    private String companyName;

    private String series;

    @Indexed
    private String lifecycle;

    private String status;

    @Indexed
    @Field("open_date")
    private LocalDate openDate;

    @Field("close_date")
    private LocalDate closeDate;

    @Field("listing_date")
    private LocalDate listingDate;

    @Field("link_removal_date")
    private LocalDate linkRemovalDate;

    private String currency;

    @Field("price_min")
    private Double priceMin;

    @Field("price_max")
    private Double priceMax;

    @Field("issue_price")
    private Double issuePrice;

    @Field("price_label")
    private String priceLabel;

    @Field("shares_offered")
    private Long sharesOffered;

    @Field("issue_size_label")
    private String issueSizeLabel;

    @Field("on_nse")
    private boolean onNse;

    @Field("on_bse")
    private boolean onBse;

    @Field("subscription_times")
    private Double subscriptionTimes;

    @Field("subscription_shares_bid")
    private Long subscriptionSharesBid;

    @Field("subscription_shares_offered")
    private Long subscriptionSharesOffered;

    private IpoSubscriptionEmbedded subscription;

    @Field("vendor_source")
    private String vendorSource;

    @Field("last_feed")
    private String lastFeed;

    @Field("synced_at")
    private Instant syncedAt;

    public static String idFor(String symbol, LocalDate openDate) {
        return symbol.trim().toUpperCase() + ":" + openDate;
    }
}
