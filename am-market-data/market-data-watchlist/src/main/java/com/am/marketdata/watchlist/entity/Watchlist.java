package com.am.marketdata.watchlist.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Entity representing a custom named Watchlist container owned by a specific user.
 * Supports user-level multi-tenancy isolation and custom display ordering.
 */
@Document(collection = "watchlists")
@CompoundIndexes({
        // Enforces unique watchlist names per user (e.g. User A can have "IT Giants", User B can also have "IT Giants")
        @CompoundIndex(name = "idx_user_watchlist_name", def = "{'userId': 1, 'name': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {

    @Id
    private String id;

    /**
     * User ID owning this watchlist (extracted from X-User-Id header).
     */
    @Field("user_id")
    private String userId;

    /**
     * Custom name of the watchlist (e.g. "My Watch List", "IT Giants").
     */
    private String name;

    /**
     * Flag indicating if this is the default auto-seeded watchlist ("My Watch List").
     * Default watchlist cannot be deleted by the user.
     */
    @Field("is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Display order sequence for sidebar rendering.
     */
    @Field("display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
