package com.am.marketdata.service;

import com.am.marketdata.service.calendar.MarketCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketHoursService {

    private final MarketCalendarService marketCalendarService;

    public boolean isMarketOpen() {
        try {
            return marketCalendarService.isMarketOpen("NSE");
        } catch (Exception e) {
            log.error("Error checking market hours: {}", e.getMessage());
            return false;
        }
    }
}
