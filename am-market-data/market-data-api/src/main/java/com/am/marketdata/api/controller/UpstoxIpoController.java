package com.am.marketdata.api.controller;

import com.am.marketdata.common.model.ipo.AsraxIpoCalendarMonthDto;
import com.am.marketdata.common.model.ipo.AsraxIpoCountsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.am.marketdata.service.ipo.UpstoxIpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * REST Controller for Upstox IPO market data endpoints.
 * Strictly a thin routing controller: contains zero business/mapping logic and delegates execution to {@link UpstoxIpoService}.
 */
@RestController
@RequestMapping("/v1/market-data/ipo")
@RequiredArgsConstructor
@Tag(
        name = "IPO Market Data",
        description = "API endpoints for fetching IPO summaries, granular details, and admin sync operations."
)
public class UpstoxIpoController {

    private final UpstoxIpoService upstoxIpoService;

    /**
     * Endpoint 1: GET /v1/ipo/upstox
     * Purpose: Retrieves a paginated summary list of IPOs filtered by status and issue type.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List Upstox IPOs",
            description = "Fetches a summary list of IPOs from Upstox filtered by status (open, upcoming, closed, listed) and issue segment (regular, sme). "
                    + "Checks Redis cache first (<= 5ms latency) and falls back seamlessly to MongoDB."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "IPO summary list retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AsraxIpoSummaryDto.class))
                    )
            )
    })
    public ResponseEntity<List<AsraxIpoSummaryDto>> getIpos(
            @Parameter(description = "Lifecycle status filter", example = "open", schema = @Schema(allowableValues = {"open", "upcoming", "closed", "listed"}))
            @RequestParam(required = false, defaultValue = "open") String status,

            @Parameter(description = "Issue segment filter (e.g., regular, sme)", example = "regular")
            @RequestParam(required = false) String issueType,

            @Parameter(description = "1-based page index", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,

            @Parameter(description = "Number of records per page (1 to 100)", example = "30")
            @RequestParam(required = false, defaultValue = "30") Integer records
    ) {
        List<AsraxIpoSummaryDto> list = upstoxIpoService.getIpos(status, issueType, pageNumber, records);
        return ResponseEntity.ok(list);
    }

    /**
     * Endpoint 2: GET /v1/ipo/upstox/{id}
     * Purpose: Retrieves 100% full granular detail for a specific IPO using its slug identifier.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get Upstox IPO Details",
            description = "Retrieves full granular details for a target IPO by slug ID, including daily bidding hours, lot sizes, price parameters, "
                    + "RHP/DRHP document links, key event schedule timeline, registrar contacts, subscription multiplier, and eligible investor categories."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "IPO detail object retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsraxIpoDetailsDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "IPO not found")
    })
    public ResponseEntity<AsraxIpoDetailsDto> getIpoDetails(
            @Parameter(description = "Target IPO slug identifier", example = "shree-tnb-polymers-limited-ipo", required = true)
            @PathVariable String id
    ) {
        return upstoxIpoService.getIpoDetails(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint 3: POST /v1/ipo/upstox/admin/sync
     * Purpose: Administrative trigger to run immediate, on-demand synchronization of Upstox IPO data.
     */
    @PostMapping(value = "/admin/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Trigger Manual Upstox IPO Sync (Admin)",
            description = "Triggers an immediate on-demand synchronization of Upstox IPO summaries and details into MongoDB upstox_ipos collection and pre-warms Redis cache."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sync completed successfully")
    })
    public ResponseEntity<Map<String, Object>> triggerSync(
            @Parameter(description = "Status filter to sync (open, upcoming, closed, listed, all)", example = "open")
            @RequestParam(required = false, defaultValue = "open") String status
    ) {
        int synced = upstoxIpoService.syncUpstoxIpos(status);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Upstox IPO sync completed successfully",
                "syncedCount", synced,
                "targetStatus", status
        ));
    }

    /**
     * Endpoint 4: GET /v1/market-data/ipo/counts

     * Purpose: Retrieves summary counts of IPOs categorized by status (open, upcoming, closed, closingToday, listed, total).
     */
    @GetMapping(value = "/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get Upstox IPO Summary Counts",
            description = "Retrieves summary counts of IPOs categorized by lifecycle status (open, upcoming, closed, closingToday, listed, total). "
                    + "Served from Redis cache (< 1ms latency) and pre-computed during nightly sync and admin sync."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "IPO summary counts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsraxIpoCountsDto.class)
                    )
            )
    })
    public ResponseEntity<AsraxIpoCountsDto> getIpoCounts() {
        AsraxIpoCountsDto counts = upstoxIpoService.getIpoCounts();
        return ResponseEntity.ok(counts);
    }

    /**
     * Endpoint 5: GET /v1/market-data/ipo/calendar
     * Purpose: Retrieves monthly calendar of IPO listing events for current or next month in Asia/Kolkata.
     */
    @GetMapping(value = "/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get Monthly IPO Calendar",
            description = "Retrieves IPO listing events occurring in a target calendar month (current or immediately following month in Asia/Kolkata), "
                    + "including listing dates, issue status (UPCOMING or LISTED), listing price, and listing exchange. "
                    + "Populates date markers and anchored popups on the IPO calendar."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "IPO calendar month retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AsraxIpoCalendarMonthDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid month or year parameter, or month outside allowed range"
            )
    })
    public ResponseEntity<AsraxIpoCalendarMonthDto> getIpoCalendarMonth(
            @Parameter(description = "Target calendar year (e.g., 2026)", example = "2026", required = true)
            @RequestParam int year,

            @Parameter(description = "Target calendar month (1-12)", example = "10", required = true)
            @RequestParam int month
    ) {
        if (month < 1 || month > 12) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }
        try {
            AsraxIpoCalendarMonthDto result = upstoxIpoService.getIpoCalendarMonth(year, month);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
