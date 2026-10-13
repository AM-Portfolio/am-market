package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoSubscriptionEmbedded {

    private String series;

    @Field("updated_at")
    private OffsetDateTime updatedAt;

    @Field("overall_times")
    private Double overallTimes;

    @Field("overall_shares_bid")
    private Long overallSharesBid;

    @Field("overall_shares_offered")
    private Long overallSharesOffered;

    @Builder.Default
    private List<IpoSubscriptionCategoryEmbedded> categories = new ArrayList<>();

    @Field("synced_at")
    private Instant syncedAt;
}
