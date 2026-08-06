package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoIssue", description = "Canonical IPO issue view")
public class IpoIssueView {

    @Schema(description = "Stable issue id: SYMBOL:openDate", example = "JNPR:2026-07-30")
    private String id;

    @Schema(description = "NSE symbol", example = "JNPR")
    private String symbol;

    @Schema(description = "NSE HTML slug when available", example = "juniper-green-energy")
    private String slug;

    @Schema(description = "Company name", example = "Juniper Green Energy Limited")
    private String companyName;

    @Schema(description = "Security series", example = "EQ")
    private String series;

    @Schema(description = "Lifecycle bucket", example = "CURRENT", allowableValues = {"UPCOMING", "CURRENT", "PAST"})
    private String lifecycle;

    @Schema(description = "Vendor status mapped to canonical enum", example = "ACTIVE", allowableValues = {"ACTIVE", "FORTHCOMING", "CLOSED", "UNKNOWN"})
    private String status;

    private IpoIssueWindow window;
    private IpoIssuePricing pricing;
    private IpoIssueSize size;
    private IpoExchanges exchanges;
    private IpoSubscriptionSummary subscriptionSummary;

    @Schema(description = "Full subscription tree; present on detail endpoints when synced")
    private IpoSubscriptionView subscription;

    private IpoVendor vendor;

    @Schema(description = "Last local upsert time", example = "2026-08-04T10:00:00Z")
    private Instant syncedAt;
}
