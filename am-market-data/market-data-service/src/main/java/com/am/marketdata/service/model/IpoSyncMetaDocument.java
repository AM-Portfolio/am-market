package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ipo_sync_meta")
public class IpoSyncMetaDocument {

    @Id
    private String id;

    @Field("last_full_sync_at")
    private Instant lastFullSyncAt;

    @Field("last_error")
    private String lastError;

    @Field("last_count")
    private Integer lastCount;

    @Field("last_trigger")
    private String lastTrigger;

    private String source;
}
