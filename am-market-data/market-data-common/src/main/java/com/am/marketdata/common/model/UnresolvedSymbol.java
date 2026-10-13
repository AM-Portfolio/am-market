package com.am.marketdata.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "unresolved_symbols")
public class UnresolvedSymbol {

    @Id
    private String id;

    @Indexed(unique = true)
    private String symbol;

    @Field("first_requested_at")
    private Date firstRequestedAt;

    @Field("last_requested_at")
    private Date lastRequestedAt;

    @Field("request_count")
    private int requestCount;

    private boolean resolved;
}
