package com.am.marketdata.api.controller;

import com.am.marketdata.common.model.ipo.AsraxIpoCalendarEntryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoCalendarMonthDto;
import com.am.marketdata.common.model.ipo.IpoCalendarDataFreshness;
import com.am.marketdata.common.model.ipo.IpoCalendarDisplayStatus;
import com.am.marketdata.service.ipo.UpstoxIpoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpstoxIpoControllerTest {

    @Mock
    private UpstoxIpoService upstoxIpoService;

    private UpstoxIpoController controller;

    @BeforeEach
    void setUp() {
        controller = new UpstoxIpoController(upstoxIpoService);
    }

    @Test
    void calendar_delegatesToServiceAndReturns200() {
        AsraxIpoCalendarMonthDto dto = AsraxIpoCalendarMonthDto.builder()
                .year(2026)
                .month(10)
                .dataFreshness(IpoCalendarDataFreshness.FRESH)
                .lastSuccessfulSyncAt("2026-10-27T10:00:00Z")
                .entries(List.of(
                        AsraxIpoCalendarEntryDto.builder()
                                .id("test-ipo")
                                .companyName("Test IPO")
                                .status(IpoCalendarDisplayStatus.UPCOMING)
                                .listingDate("2026-10-29")
                                .listingPrice(250.0)
                                .listingExchange("NSE")
                                .build()
                ))
                .build();

        when(upstoxIpoService.getIpoCalendarMonth(2026, 10)).thenReturn(dto);

        ResponseEntity<AsraxIpoCalendarMonthDto> response = controller.getIpoCalendarMonth(2026, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2026, response.getBody().getYear());
        assertEquals(10, response.getBody().getMonth());
        assertEquals(1, response.getBody().getEntries().size());
        verify(upstoxIpoService).getIpoCalendarMonth(2026, 10);
    }

    @Test
    void calendar_invalidMonth_throwsBadRequest() {
        ResponseStatusException ex0 = assertThrows(ResponseStatusException.class,
                () -> controller.getIpoCalendarMonth(2026, 0));
        assertEquals(HttpStatus.BAD_REQUEST, ex0.getStatusCode());

        ResponseStatusException ex13 = assertThrows(ResponseStatusException.class,
                () -> controller.getIpoCalendarMonth(2026, 13));
        assertEquals(HttpStatus.BAD_REQUEST, ex13.getStatusCode());
    }

    @Test
    void calendar_serviceThrowsIllegalArgument_mapsToBadRequest() {
        when(upstoxIpoService.getIpoCalendarMonth(2026, 8))
                .thenThrow(new IllegalArgumentException("Month outside allowed range"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getIpoCalendarMonth(2026, 8));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
