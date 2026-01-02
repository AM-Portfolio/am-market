// package com.am.marketdata.api.controller;

// import java.util.Optional;

// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.am.common.investment.model.board.BoardOfDirectors;
// import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
// import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
// import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
// import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
// import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
// import com.am.marketdata.processor.service.StockPerformaceService;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.Parameter;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import io.swagger.v3.oas.annotations.media.Content;
// import io.swagger.v3.oas.annotations.media.Schema;
// import io.swagger.v3.oas.annotations.responses.ApiResponse;
// import io.swagger.v3.oas.annotations.responses.ApiResponses;
// import jakarta.validation.constraints.NotBlank;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * REST controller for stock financial and corporate information
//  * Provides endpoints for retrieving various financial statements, board of directors,
//  * quarterly results, and dividend information for stocks
//  */
// @RestController
// @RequestMapping("/api/v1/stocks")
// @Tag(name = "Stock Portfolio", description = "APIs for retrieving stock financial statements, board of directors, and other corporate information")
// @Slf4j
// @Validated
// @RequiredArgsConstructor
// public class StockPortfolioController {
    
//     private final StockPerformaceService stockFinancialPerformaceService;

//     /**
//      * Get board of directors information for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Board of directors information
//      */
//     @GetMapping(value = "/{symbol}/board-of-directors", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get board of directors for a stock",
//             description = "Retrieves the current board of directors information for a specified stock including names, positions, and tenure")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved board of directors", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardOfDirectors.class))),
//         @ApiResponse(responseCode = "404", description = "No board of directors found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<BoardOfDirectors> getBoardOfDirectors(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching board of directors for symbol: {}", symbol);
        
//         Optional<BoardOfDirectors> directors = stockFinancialPerformaceService.fetchAndProcessStockOverview(symbol);
        
//         if (directors.isEmpty()) {
//             log.warn("No board of directors found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved board of directors for symbol: {}", symbol);
//         return ResponseEntity.ok(directors.get());
//     }


//     /**
//      * Get quarterly financial results for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Quarterly financial results
//      */
//     @GetMapping(value = "/{symbol}/quaterly-financials", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get quarterly financials for a stock",
//             description = "Retrieves the quarterly financial results for a specified stock including revenue, profit, EPS, and other key metrics")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved quarterly financials", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuaterlyResult.class))),
//         @ApiResponse(responseCode = "404", description = "No quarterly financials found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<QuaterlyResult> getQuaterlyFinancials(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching quaterly financials for symbol: {}", symbol);
        
//         Optional<QuaterlyResult> quaterlyResult = stockFinancialPerformaceService.fetchAndProcessQuaterlyFinancials(symbol);
        
//         if (quaterlyResult.isEmpty()) {
//             log.warn("No quaterly financials found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved quaterly financials for symbol: {}", symbol);
//         return ResponseEntity.ok(quaterlyResult.get());
//     }

    
//     /**
//      * Get balance sheet for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Balance sheet information
//      */
//     @GetMapping(value = "/{symbol}/balance-sheet", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get balance sheet for a stock",
//             description = "Retrieves the latest balance sheet for a specified stock including assets, liabilities, and equity information")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved balance sheet", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockBalanceSheet.class))),
//         @ApiResponse(responseCode = "404", description = "No balance sheet found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<StockBalanceSheet> getBalanceSheet(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching balance sheet for symbol: {}", symbol);
        
//         Optional<StockBalanceSheet> balanceSheet = stockFinancialPerformaceService.fetchAndProcessBalanceSheet(symbol);
        
//         if (balanceSheet.isEmpty()) {
//             log.warn("No balance sheet found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved balance sheet for symbol: {}", symbol);
//         return ResponseEntity.ok(balanceSheet.get());
//     }

//     /**
//      * Get profit and loss statement for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Profit and loss statement
//      */
//     @GetMapping(value = "/{symbol}/profit-and-loss", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get profit and loss statement for a stock",
//             description = "Retrieves the latest profit and loss statement for a specified stock including revenue, expenses, and net profit")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved profit and loss statement", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockProfitAndLoss.class))),
//         @ApiResponse(responseCode = "404", description = "No profit and loss statement found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<StockProfitAndLoss> getProfitAndLoss(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching profit and loss for symbol: {}", symbol);
        
//         Optional<StockProfitAndLoss> profitAndLoss = stockFinancialPerformaceService.fetchAndProcessProfitAndLoss(symbol);
        
//         if (profitAndLoss.isEmpty()) {
//             log.warn("No profit and loss found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved profit and loss for symbol: {}", symbol);
//         return ResponseEntity.ok(profitAndLoss.get());
//     }

//     /**
//      * Get cash flow statement for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Cash flow statement
//      */
//     @GetMapping(value = "/{symbol}/cash-flow", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get cash flow statement for a stock",
//             description = "Retrieves the latest cash flow statement for a specified stock including operating, investing, and financing activities")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved cash flow statement", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockCashFlow.class))),
//         @ApiResponse(responseCode = "404", description = "No cash flow statement found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<StockCashFlow> getCashFlow(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching cash flow for symbol: {}", symbol);
        
//         Optional<StockCashFlow> cashFlow = stockFinancialPerformaceService.fetchAndProcessCashFlow(symbol);
        
//         if (cashFlow.isEmpty()) {
//             log.warn("No cash flow found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved cash flow for symbol: {}", symbol);
//         return ResponseEntity.ok(cashFlow.get());
//     }

//     /**
//      * Get dividend information for a stock
//      * 
//      * @param symbol Stock trading symbol
//      * @return Dividend factsheet information
//      */
//     @GetMapping(value = "/{symbol}/factsheet-dividend", produces = MediaType.APPLICATION_JSON_VALUE)
//     @Operation(summary = "Get dividend information for a stock",
//             description = "Retrieves dividend history and information for a specified stock including dividend amounts, dates, and yield")
//     @ApiResponses(value = {
//         @ApiResponse(responseCode = "200", description = "Successfully retrieved dividend information", 
//             content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockFactSheetDividend.class))),
//         @ApiResponse(responseCode = "404", description = "No dividend information found for the symbol"),
//         @ApiResponse(responseCode = "500", description = "Internal server error")
//     })
//     public ResponseEntity<StockFactSheetDividend> getFactsheetDividend(
//             @Parameter(description = "Stock symbol", required = true)
//             @PathVariable("symbol") @NotBlank String symbol) {
        
//         log.info("Fetching factsheet dividend for symbol: {}", symbol);
        
//         Optional<StockFactSheetDividend> factsheetDividend = stockFinancialPerformaceService.fetchAndProcessFactSheetDividend(symbol);
        
//         if (factsheetDividend.isEmpty()) {
//             log.warn("No factsheet dividend found for symbol: {}", symbol);
//             return ResponseEntity.notFound().build();
//         }
        
//         log.info("Successfully retrieved factsheet dividend for symbol: {}", symbol);
//         return ResponseEntity.ok(factsheetDividend.get());
//     }
// }
