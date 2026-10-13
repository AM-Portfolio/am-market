package com.am.marketdata.service.model;

import com.am.marketdata.common.model.ipo.AsraxInvestorCategoryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoRegistrarDto;
import com.am.marketdata.common.model.ipo.AsraxIpoTimelineDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document entity mapped to the dedicated "upstox_ipos" collection.
 * Isolates Upstox feed data from existing NSE scraper collections to prevent feed collisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "upstox_ipos")
@CompoundIndexes({
    @CompoundIndex(name = "status_bidding_idx", def = "{'status': 1, 'biddingStartDate': -1}"),
    @CompoundIndex(name = "status_issueType_bidding_idx", def = "{'status': 1, 'issueType': 1, 'biddingStartDate': -1}")
})
public class UpstoxIpoDocument {

    /**
     * Unique Document ID (e.g., "shree-tnb-polymers-limited-ipo").
     */
    @Id
    private String id;

    @Indexed
    private String symbol;

    private String companyName;

    @Indexed
    private String status;

    private String isin;

    @Indexed
    private String issueType;

    private Double issueSizeCr;
    private String industry;
    private Double minimumPrice;
    private Double maximumPrice;
    private String biddingStartDate;
    private String biddingEndDate;
    private String dailyStartTime;
    private String dailyEndTime;
    private Double faceValue;
    private Double tickSize;
    private Integer lotSize;
    private Integer minimumQuantity;
    private Double cutOffPrice;
    private Double listingPrice;
    private String listingExchange;
    private String rhpUrl;
    private String drhpUrl;

    private AsraxIpoTimelineDto timeline;
    private AsraxIpoRegistrarDto registrarInfo;
    private String totalSubscription;
    private List<AsraxInvestorCategoryDto> eligibleInvestors;

    /**
     * Timestamp when document was last updated in MongoDB.
     */
    private Instant updatedAt;
}
