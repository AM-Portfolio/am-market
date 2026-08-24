package com.am.marketdata.api.controller;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.api.service.StockIndicesService;
import com.am.marketdata.api.service.global.GlobalIndexService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST controller for market indices data — both Indian (NSE) and Global.
 *
 * <p><b>Dynamic Routing Strategy:</b>
 * This controller uses in-memory symbol classification to route requests to the
 * correct service without any query parameters. When a client sends a batch request:
 * <ol>
 *   <li>Each symbol is validated against both NSE config and the global MongoDB config.</li>
 *   <li>Unknown symbols return a 400 Bad Request immediately (before any service call).</li>
 *   <li>Known symbols are split into {@code indianSymbols} and {@code globalSymbols}.</li>
 *   <li>Both lists are fetched concurrently using CompletableFuture.</li>
 *   <li>Results are merged and re-sorted to match the EXACT order of the original request.</li>
 * </ol>
 *
 * <p><b>Backward Compatibility Guarantee:</b>
 * <ul>
 *   <li>{@code GET /v1/indices/available} is UNTOUCHED — returns only NSE indices.</li>
 *   <li>Existing Indian-only callers of {@code POST /v1/indices/batch} are unaffected
 *       because all their symbols route to {@code StockIndicesService} unchanged.</li>
 *   <li>The response DTO ({@link StockIndicesMarketData}) is identical for both Indian
 *       and global indices; only Indian-specific fields will be null for global indices.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/indices")
@RequiredArgsConstructor
@Tag(name = "Indices", description = "APIs for retrieving market data for various indices")
public class MarketIndexController {

    private final AppLogger log = AppLogger.getLogger();

    /** Indian NSE index configuration — loaded from YAML at startup (in-memory). */
    private final com.am.marketdata.scraper.config.NSEIndicesConfig nseIndicesConfig;

    /** Handles fetching for Indian NSE indices (UNTOUCHED from original implementation). */
    private final StockIndicesService stockIndicesService;

    /** Handles fetching for foreign/global market indices. */
    private final GlobalIndexService globalIndexService;

    /**
     * MongoDB repository for global index configuration.
     * Used to:
     * 1. Validate whether an unknown symbol belongs to the global universe.
     * 2. Build the response for GET /v1/indices/global/available.
     */
    private final GlobalIndexConfigRepository globalIndexConfigRepository;

    /**
     * Returns the list of available Indian NSE indices.
     *
     * <p><b>UNCHANGED:</b> This endpoint is preserved exactly as-is for full
     * backward compatibility with all existing microservices. It returns only
     * {@code broad} and {@code sector} NSE indices.
     *
     * <p>For global indices, use {@code GET /v1/indices/global/available}.
     *
     * @return map with "broad" and "sector" keys containing NSE index lists
     */
    @GetMapping(value = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get available NSE indices", description = "Retrieves the list of available NSE indices (Broad and Sector). For global indices, use /v1/indices/global/available.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indices retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.List.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, List<String>>> getAvailableIndices() {
        log.info("getAvailableIndices", "Fetching available NSE indices");
        Map<String, List<String>> indices = new HashMap<>();
        indices.put("broad", nseIndicesConfig.getBroadMarketIndices());
        indices.put("sector", nseIndicesConfig.getSectorIndices());
        return ResponseEntity.ok(indices);
    }

    /**
     * Returns the list of available global (foreign) market indices.
     *
     * <p>Separate from {@code /available} to prevent existing microservices
     * from accidentally discovering global symbols and fetching them without
     * being aware of the foreign market context (different hours, no constituents, etc.).
     *
     * <p>Reads from the {@code global_index_config} MongoDB collection,
     * which is seeded on startup by {@code GlobalIndexSeeder} and updated
     * daily by the Upstox Instruments refresh job.
     *
     * @return map with "global" key containing the list of global index symbols
     */
    @GetMapping(value = "/global/available", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get available global indices", description = "Retrieves the list of available global/foreign market indices tracked by the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Global indices retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, List<String>>> getAvailableGlobalIndices() {
        log.info("getAvailableGlobalIndices", "Fetching available global market indices from MongoDB");

        List<String> globalSymbols = globalIndexConfigRepository.findAll()
                .stream()
                .map(GlobalIndexConfigDocument::getSymbol)
                .collect(Collectors.toList());

        Map<String, List<String>> response = new HashMap<>();
        response.put("global", globalSymbols);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches the latest market data for a batch of index symbols (Indian and/or Global).
     *
     * <p><b>Dynamic Routing Logic:</b>
     * <pre>
     *   Input: ["NIFTY 50", "DJI", "NIFTY BANK", "UNKNOWN"]
     *
     *   Step 1 — Validate: "UNKNOWN" not in NSE config or global config → 400 Bad Request
     *
     *   Step 2 — Split:
     *     indianSymbols = ["NIFTY 50", "NIFTY BANK"]   → goes to StockIndicesService
     *     globalSymbols = ["DJI"]                        → goes to GlobalIndexService
     *
     *   Step 3 — Parallel fetch (CompletableFuture):
     *     StockIndicesService.getLatestIndicesData(indianSymbols)
     *     GlobalIndexService.getLatestGlobalIndices(globalSymbols)
     *
     *   Step 4 — Merge + Sort back to original order:
     *     ["NIFTY 50", "DJI", "NIFTY BANK"]
     *
     *   Step 5 — Return unified List&lt;StockIndicesMarketData&gt;
     * </pre>
     *
     * <p><b>Order Guarantee:</b>
     * The response list is re-sorted to match the exact order of the input request.
     * This prevents index-based mapping bugs in frontend widgets.
     *
     * <p><b>Indian-only Callers:</b>
     * If the input contains only Indian symbols, {@code globalSymbols} will be empty
     * and {@code GlobalIndexService} is never called. The behavior is identical to
     * the original implementation.
     *
     * @param indexSymbols  the list of index symbols to fetch
     * @param forceRefresh  whether to bypass caches and fetch fresh data from source
     * @return unified list of market data, in the same order as the input
     */
    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get latest market data for multiple indices (Indian and/or Global)",
            description = "Batch endpoint supporting both NSE and global indices. Dynamically routes to the correct service based on symbol classification. Unknown symbols return 400."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Indices data retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = StockIndicesMarketData.class)
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "One or more unknown/invalid index symbols in the request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getLatestIndicesData(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of index symbols (NSE or global) to fetch market data for",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "array",
                                    implementation = String.class,
                                    description = "Array of valid index symbols",
                                    example = "[\"NIFTY 50\", \"NIFTY BANK\", \"DJI\"]"
                            )
                    )
            )
            @RequestBody List<String> indexSymbols,
            @Parameter(description = "Force refresh from source instead of using cache", example = "false")
            @RequestParam(value = "forceRefresh", required = false, defaultValue = "false") boolean forceRefresh) {

        String methodName = "getLatestIndicesData";
        log.info(methodName, String.format("Batch request for %d symbols, forceRefresh=%b", indexSymbols.size(), forceRefresh));

        // -------------------------------------------------------------------------
        // STEP 1: BUILD KNOWN SYMBOL SETS FOR VALIDATION AND ROUTING
        //
        // Both NSE config (in-memory YAML) and global config (MongoDB) are loaded.
        // This validation runs in nanoseconds for NSE (in-memory) and milliseconds
        // for global (MongoDB is cached by Spring Data). It does NOT add measurable
        // latency to the API response.
        // -------------------------------------------------------------------------
        Set<String> nseSymbols = buildNseSymbolSet();
        Set<String> globalSymbols = globalIndexConfigRepository.findAll()
                .stream()
                .map(GlobalIndexConfigDocument::getSymbol)
                .collect(Collectors.toSet());

        // -------------------------------------------------------------------------
        // STEP 2: VALIDATE — reject unknown symbols immediately (before any DB call)
        // Returns 400 with a descriptive error so callers can fix their request.
        // -------------------------------------------------------------------------
        List<String> unknownSymbols = indexSymbols.stream()
                .filter(s -> !nseSymbols.contains(s) && !globalSymbols.contains(s))
                .collect(Collectors.toList());

        if (!unknownSymbols.isEmpty()) {
            String error = String.format("Unknown index symbols: %s. " +
                    "Use GET /v1/indices/available (NSE) or GET /v1/indices/global/available (Global) " +
                    "to see valid symbols.", unknownSymbols);
            log.warn(methodName, "Rejecting batch request with unknown symbols: " + unknownSymbols);
            return ResponseEntity.badRequest().body(Map.of("error", error, "unknownSymbols", unknownSymbols));
        }

        // -------------------------------------------------------------------------
        // STEP 3: SPLIT into Indian and Global lists
        // If a request contains only Indian symbols, globalList is empty and
        // GlobalIndexService is never called — identical behavior to the original.
        // -------------------------------------------------------------------------
        List<String> indianList = indexSymbols.stream()
                .filter(nseSymbols::contains)
                .collect(Collectors.toList());

        List<String> globalList = indexSymbols.stream()
                .filter(globalSymbols::contains)
                .collect(Collectors.toList());

        log.info(methodName, String.format("Split: %d Indian, %d Global symbols", indianList.size(), globalList.size()));

        // -------------------------------------------------------------------------
        // STEP 4: PARALLEL FETCH
        // Both services are called concurrently to minimize total response time.
        // If only one list is non-empty, the other future completes immediately.
        // -------------------------------------------------------------------------
        final boolean finalForceRefresh = forceRefresh;

        CompletableFuture<List<StockIndicesMarketData>> indianFuture = indianList.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(() ->
                        stockIndicesService.getLatestIndicesData(indianList, finalForceRefresh));

        CompletableFuture<List<StockIndicesMarketData>> globalFuture = globalList.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : CompletableFuture.supplyAsync(() ->
                        globalIndexService.getLatestGlobalIndices(globalList));

        List<StockIndicesMarketData> indianResults;
        List<StockIndicesMarketData> globalResults;

        try {
            indianResults = indianFuture.get();
            globalResults = globalFuture.get();
        } catch (Exception e) {
            log.error(methodName, "Error fetching indices data in parallel", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch market data: " + e.getMessage()));
        }

        // -------------------------------------------------------------------------
        // STEP 5: MERGE and RE-SORT to original request order
        //
        // Build a symbol → result map, then re-order by original input list.
        // This guarantees result[0] always corresponds to indexSymbols[0],
        // preventing silent index-mapping bugs in frontend widgets that rely
        // on positional ordering.
        // -------------------------------------------------------------------------
        Map<String, StockIndicesMarketData> resultMap = new HashMap<>();
        indianResults.forEach(d -> resultMap.put(d.getIndexSymbol(), d));
        globalResults.forEach(d -> resultMap.put(d.getIndexSymbol(), d));

        List<StockIndicesMarketData> orderedResults = indexSymbols.stream()
                .map(resultMap::get)
                .filter(d -> d != null)
                .collect(Collectors.toList());

        log.info(methodName, String.format("Returning %d results for %d requested symbols",
                orderedResults.size(), indexSymbols.size()));

        return ResponseEntity.ok(orderedResults);
    }

    /**
     * Builds the complete set of known Indian NSE index symbols for fast in-memory lookup.
     *
     * <p>Combines both broad market and sector indices from the YAML-backed
     * {@code NSEIndicesConfig} bean. This set is used for O(1) symbol classification
     * in the batch routing logic.
     *
     * @return set of all NSE index symbol strings
     */
    private Set<String> buildNseSymbolSet() {
        Set<String> symbols = new java.util.HashSet<>();
        if (nseIndicesConfig.getBroadMarketIndices() != null) {
            symbols.addAll(nseIndicesConfig.getBroadMarketIndices());
        }
        if (nseIndicesConfig.getSectorIndices() != null) {
            symbols.addAll(nseIndicesConfig.getSectorIndices());
        }
        return symbols;
    }
}
