package com.am.marketdata.api.controller;

import com.am.marketdata.api.model.FundamentalAnalysisResponse;
import com.am.marketdata.api.model.FundamentalRatiosResponse;
import com.am.marketdata.api.service.FundamentalAnalysisService;
import com.am.marketdata.common.model.fundamental.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Stock Fundamental Analysis.
 * Adheres to `/v1/**` routing and unified JSON response conventions.
 */
@Slf4j
@RestController
@RequestMapping("/v1/fundamentals")
@Tag(name = "Fundamental Analysis", description = "APIs for company profile, financial statements, valuation, ratios, and peers")
@RequiredArgsConstructor
public class FundamentalAnalysisController {

    private final FundamentalAnalysisService fundamentalAnalysisService;

        /**
         * Retrieves unified fundamental analysis data for a stock by trading symbol.
         *
         * @param symbol Trading symbol (e.g. "TCS", "ITC", "INFY", "RELIANCE").
         * @return 200 OK with {@link FundamentalAnalysisResponse}, or 404 if not found.
         */
        @GetMapping(value = "/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFundamentalAnalysis", summary = "Get unified fundamental analysis for a stock", description = "Retrieves complete fundamental profile, key ratios, P&L statements, balance sheets, cash flows, shareholding pattern, corporate actions, competitor peers, and health analytics by stock symbol (e.g., TCS, INFY, ITC).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Fundamental data retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Bad Request Example", value = "{\"error\": \"INVALID_PARAMETER\", \"message\": \"Stock symbol parameter is required\"}"))),
                        @ApiResponse(responseCode = "404", description = "Fundamental data not found for given stock symbol", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Not Found Example", value = "{\"error\": \"NOT_FOUND\", \"message\": \"No fundamental data available for stock: UNKNOWN\"}"))),
                        @ApiResponse(responseCode = "500", description = "Internal server error occurred while retrieving fundamental data", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Server Error Example", value = "{\"error\": \"INTERNAL_ERROR\", \"message\": \"Failed to retrieve fundamental analysis\"}")))
        })
        public ResponseEntity<FundamentalAnalysisResponse> getFundamentals(
                        @Parameter(name = "symbol", description = "NSE / BSE Stock Trading Symbol (case-insensitive, e.g., 'TCS', 'tcs', 'INFY')", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getFundamentals(symbol));
        }

        @GetMapping(value = "/{symbol}/profile", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCompanyProfile", summary = "Get company profile", description = "Retrieves company overview, sector, market cap, and live price by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Company profile retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.CompanyOverviewSection.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Company profile not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalysisResponse.CompanyOverviewSection> getCompanyProfile(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getCompanyProfile(symbol));
        }

        @GetMapping(value = "/{symbol}/ratios", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFinancialRatios", summary = "Get valuation and profitability ratios", description = "Retrieves PE, PB, ROE, ROA, ROCE, EV/EBITDA compared with sector benchmarks by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Key ratios retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalRatiosResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Ratios not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalRatiosResponse> getRatios(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getRatios(symbol));
        }

        @GetMapping(value = "/{symbol}/financials", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFinancialStatements", summary = "Get financial statements", description = "Retrieves multi-year income statements, balance sheets, and cash flow statements by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Financial statements retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.FinancialsSection.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Financials not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalysisResponse.FinancialsSection> getFinancials(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getFinancials(symbol));
        }

        @GetMapping(value = "/{symbol}/shareholding", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getShareholdingPattern", summary = "Get shareholding patterns", description = "Retrieves historical quarterly ownership trends (Promoters, FII, DII, Mutual Funds, Retail) by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Shareholding pattern retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ShareholdingQuarterEntry.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Shareholding pattern not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<ShareholdingQuarterEntry>> getShareholding(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getShareholding(symbol));
        }

        @GetMapping(value = "/{symbol}/corporate-actions", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCorporateActions", summary = "Get corporate actions", description = "Retrieves dividends, stock splits, bonuses, and rights announcements by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Corporate actions retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CorporateActionEntry.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Corporate actions not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<CorporateActionEntry>> getCorporateActions(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getCorporateActions(symbol));
        }

        @GetMapping(value = "/{symbol}/peers", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCompetitorPeers", summary = "Get competitor peers", description = "Retrieves industry peer comparisons and competitor valuations by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Competitor peers retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CompetitorPeer.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Peers not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<CompetitorPeer>> getPeers(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getPeers(symbol));
        }

        @GetMapping(value = "/{symbol}/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getHealthAnalytics", summary = "Get derived health analytics", description = "Retrieves calculated financial health analytics (Current Ratio, CFO/PAT, CAGRs) by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Health analytics retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalytics.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Analytics not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalytics> getAnalytics(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.ok(fundamentalAnalysisService.getAnalytics(symbol));
        }
}
