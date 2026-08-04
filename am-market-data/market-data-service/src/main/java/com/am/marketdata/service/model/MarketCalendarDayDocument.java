package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "market_calendar_days")
public class MarketCalendarDayDocument {

    @Id
    private String id;

    private LocalDate date;

    private String exchange;

    private String description;

    @Field("holiday_type")
    private String holidayType;

    private boolean closed;

    @Field("session_start")
    private LocalTime sessionStart;

    @Field("session_end")
    private LocalTime sessionEnd;

    private String source;

    @Field("synced_at")
    private Instant syncedAt;

    public static String idFor(String exchange, LocalDate date) {
        return exchange.toUpperCase() + ":" + date;
    }
}
