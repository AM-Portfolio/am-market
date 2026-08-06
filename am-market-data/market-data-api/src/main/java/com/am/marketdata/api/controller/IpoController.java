package com.am.marketdata.api.controller;

import com.am.marketdata.api.model.ipo.IpoIssueListResponse;
import com.am.marketdata.api.model.ipo.IpoIssueView;
import com.am.marketdata.api.model.ipo.IpoSubscriptionView;
import com.am.marketdata.api.model.ipo.IpoSyncMetaView;
import com.am.marketdata.api.service.IpoApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/ipo")
@RequiredArgsConstructor
@Tag(
        name = "IPO",
        description = "NSE IPO issues and subscription (bid) details. Issues are keyed by symbol + openDate "
                + "(id format SYMBOL:yyyy-MM-dd). Lifecycle values: UPCOMING, CURRENT, PAST.")
public class IpoController {

    private final IpoApiService ipoApiService;

    @GetMapping(value = "/meta/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get IPO sync metadata",
            description = "Returns last sync timestamps, counts, and errors per feed "
                    + "(PAST, CURRENT, UPCOMING, SUBSCRIPTION).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sync metadata list",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = IpoSyncMetaView.class))))
    })
    public ResponseEntity<List<IpoSyncMetaView>> syncMeta() {
        return ResponseEntity.ok(ipoApiService.syncMeta());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List IPO issues",
            description = "Lists IPO issues with optional filters. List items include subscriptionSummary only; "
                    + "use detail or subscription endpoints for category bid trees.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "IPO issue list",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IpoIssueListResponse.class)))
    })
    public ResponseEntity<IpoIssueListResponse> list(
            @Parameter(description = "Lifecycle filter", example = "CURRENT", schema = @Schema(allowableValues = {"UPCOMING", "CURRENT", "PAST"}))
            @RequestParam(required = false) String lifecycle,
            @Parameter(description = "Series filter (e.g. EQ, SME)", example = "EQ")
            @RequestParam(required = false) String series,
            @Parameter(description = "Case-insensitive search on symbol or company name", example = "juniper")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ipoApiService.list(lifecycle, series, q));
    }

    @GetMapping(value = "/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List IPO issues by symbol",
            description = "Returns all IPO windows for a symbol (same symbol can appear more than once across years). "
                    + "Includes embedded subscription when available.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Issues for symbol",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IpoIssueListResponse.class)))
    })
    public ResponseEntity<IpoIssueListResponse> bySymbol(
            @Parameter(description = "NSE IPO symbol", example = "JNPR", required = true)
            @PathVariable String symbol) {
        return ResponseEntity.ok(ipoApiService.bySymbol(symbol));
    }

    @GetMapping(value = "/{symbol}/{openDate}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get IPO issue detail",
            description = "Returns one issue identified by symbol and open date, including subscription when present.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Issue detail",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IpoIssueView.class))),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<IpoIssueView> detail(
            @Parameter(description = "NSE IPO symbol", example = "JNPR", required = true)
            @PathVariable String symbol,
            @Parameter(description = "Issue open date (ISO-8601)", example = "2026-07-30", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate) {
        return ipoApiService
                .detail(symbol, openDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{symbol}/{openDate}/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get IPO subscription / bid details",
            description = "Category-wise subscription tree (QIB/NII/RII and children) with overall times and shares.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subscription detail",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IpoSubscriptionView.class))),
            @ApiResponse(responseCode = "404", description = "Issue or subscription not found")
    })
    public ResponseEntity<IpoSubscriptionView> subscription(
            @Parameter(description = "NSE IPO symbol", example = "JNPR", required = true)
            @PathVariable String symbol,
            @Parameter(description = "Issue open date (ISO-8601)", example = "2026-07-30", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate) {
        return ipoApiService
                .subscription(symbol, openDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
