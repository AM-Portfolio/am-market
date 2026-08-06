package com.am.marketdata.api.service;

import com.am.marketdata.api.model.calendar.MarketCalendarApiMapper;
import com.am.marketdata.api.model.calendar.MarketCalendarHolidaysResponse;
import com.am.marketdata.api.model.calendar.MarketSessionTimingResponse;
import com.am.marketdata.api.model.calendar.MarketStatusResponse;
import com.am.marketdata.service.calendar.MarketCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class MarketCalendarApiService {

    private final MarketCalendarService marketCalendarService;

    public MarketCalendarHolidaysResponse getHolidays(String exchange, Integer year) {
        int y = year != null ? year : Year.now().getValue();
        return MarketCalendarApiMapper.toHolidays(marketCalendarService.getHolidays(exchange, y));
    }

    public MarketCalendarHolidaysResponse getHoliday(String exchange, LocalDate date) {
        return MarketCalendarApiMapper.toHolidays(marketCalendarService.getHoliday(exchange, date));
    }

    public MarketSessionTimingResponse getTimings(String exchange, LocalDate date) {
        return MarketCalendarApiMapper.toTiming(marketCalendarService.getTimings(exchange, date));
    }

    public MarketStatusResponse getStatus(String exchange) {
        return MarketCalendarApiMapper.toStatus(marketCalendarService.getStatus(exchange));
    }
}
