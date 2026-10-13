package com.am.marketdata.api.model.calendar;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MarketCalendarHolidaysResponse", description = "Holiday list for an exchange and year")
public class MarketCalendarHolidaysResponse {

    @ArraySchema(schema = @Schema(implementation = MarketCalendarDayView.class))
    @Builder.Default
    private List<MarketCalendarDayView> data = new ArrayList<>();

    @Schema(description = "Response metadata")
    private MarketCalendarMetaView meta;
}
