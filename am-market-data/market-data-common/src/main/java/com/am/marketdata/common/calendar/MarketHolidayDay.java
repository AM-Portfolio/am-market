package com.am.marketdata.common.calendar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public final class MarketHolidayDay {
    private final LocalDate date;
    private final String exchange;
    private final String description;
    private final MarketHolidayType holidayType;
    private final boolean closed;
    private final LocalTime sessionStart;
    private final LocalTime sessionEnd;

    public MarketHolidayDay(
            LocalDate date,
            String exchange,
            String description,
            MarketHolidayType holidayType,
            boolean closed,
            LocalTime sessionStart,
            LocalTime sessionEnd) {
        this.date = Objects.requireNonNull(date, "date");
        this.exchange = Objects.requireNonNull(exchange, "exchange");
        this.description = description;
        this.holidayType = Objects.requireNonNull(holidayType, "holidayType");
        this.closed = closed;
        this.sessionStart = sessionStart;
        this.sessionEnd = sessionEnd;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getExchange() {
        return exchange;
    }

    public String getDescription() {
        return description;
    }

    public MarketHolidayType getHolidayType() {
        return holidayType;
    }

    public boolean isClosed() {
        return closed;
    }

    public LocalTime getSessionStart() {
        return sessionStart;
    }

    public LocalTime getSessionEnd() {
        return sessionEnd;
    }
}
