package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoSubscriptionCategoryEmbedded {

    private String code;
    private String name;

    @Field("sr_no")
    private String srNo;

    @Field("shares_offered")
    private Long sharesOffered;

    @Field("shares_bid")
    private Long sharesBid;

    private Double times;

    @Builder.Default
    private List<IpoSubscriptionCategoryEmbedded> children = new ArrayList<>();
}
