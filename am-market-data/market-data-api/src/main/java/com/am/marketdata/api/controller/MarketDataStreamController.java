package com.am.marketdata.api.controller;

import com.am.marketdata.api.model.StreamConnectRequest;
import com.am.marketdata.api.model.StreamConnectResponse;
import com.am.marketdata.api.service.MarketDataPollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/market-data/stream")
@RequiredArgsConstructor
@Tag(name = "Market Data Stream", description = "APIs for managing market data WebSocket streams")
public class MarketDataStreamController {

    private final MarketDataPollingService pollingService;

    @PostMapping("/connect")
    @Operation(summary = "Connect to market data stream", description = "Initiates a WebSocket connection for the specified provider and instruments")
    public ResponseEntity<String> connect(@RequestBody StreamConnectRequest request) {
        try {
            log.info("Received stream connection request | Provider: {} | Instruments: {} | TimeFrame: {}",
                    request.getProvider(), request.getInstrumentKeys(), request.getTimeFrame());

            // Delegate logic to service which handles market hours check and fallback
            StreamConnectResponse response = pollingService.initiateStream(request);

            if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                return ResponseEntity.ok(response.getMessage());
            } else {
                return ResponseEntity.internalServerError().body("Failed: " + response.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to initiate stream connection: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/initiate")
    @Operation(summary = "Connect to market data stream", description = "Initiates a WebSocket connection and returns a structured response")
    public ResponseEntity<com.am.marketdata.api.model.StreamConnectResponse> initiate(
            @RequestBody StreamConnectRequest request) {
        try {
            log.info("Received stream connection request (initiate) for timeFrame: {}",
                    request.getTimeFrame());

            // Delegate to service, which returns the structured response with initial data
            // This now includes market hour checks and fallback logic
            return ResponseEntity.ok(pollingService.initiateStream(request));
        } catch (Exception e) {
            log.error("Failed to initiate stream connection: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(com.am.marketdata.api.model.StreamConnectResponse.builder()
                    .status("FAILED")
                    .message("Failed: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect market data stream", description = "Disconnects the WebSocket stream for the specified provider")
    public ResponseEntity<String> disconnect(@RequestParam String provider) {
        try {
            pollingService.disconnectStream(provider);
            return ResponseEntity.ok("Disconnected successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
