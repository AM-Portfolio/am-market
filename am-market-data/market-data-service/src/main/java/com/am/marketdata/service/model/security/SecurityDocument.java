package com.am.marketdata.service.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "securities")
public class SecurityDocument implements Serializable {

    @Id
    private String id;

    private SecurityKey key;

    private SecurityMetadata metadata;

    private Audit audit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityKey implements Serializable {
        private String symbol;
        private String isin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityMetadata implements Serializable {
        @Field("company_name")
        private String companyName;

        private String sector;
        private String industry;

        @Field("market_cap_value")
        private Long marketCapValue;

        @Field("market_cap_type")
        private String marketCapType;

        private String status; // Active, Delisted, Suspended, etc.
        private String group; // A, B, T, X, etc. (BSE/NSE classification)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Audit implements Serializable {
        @Field("created_at")
        private Instant createdAt;

        private Long version;
    }
}
