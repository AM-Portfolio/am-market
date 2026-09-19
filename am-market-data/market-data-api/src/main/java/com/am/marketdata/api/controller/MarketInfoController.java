package com.am.marketdata.api.controller;

import com.am.marketdata.api.service.MarketInfoService;
import com.am.marketdata.common.model.marketinfo.ChangeOiResponse;
import com.am.marketdata.common.model.marketinfo.FlowsOverviewResponse;
import com.am.marketdata.common.model.marketinfo.InstitutionalFlowResponse;
import com.am.marketdata.common.model.marketinfo.OiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/market-info")
@RequiredArgsConstructor
@Tag(name = "Market Information", description = "Institutional flows and index open interest")
public class MarketInfoController {
    private final MarketInfoService marketInfoService;

    @GetMapping("/fii")
    @Operation(summary = "Get FII flows")
    public InstitutionalFlowResponse getFii(
            @RequestParam(defaultValue = "1D") String interval,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) List<String> dataTypes) {
        return marketInfoService.getFii(dataTypes, interval, from);
    }

    @GetMapping("/dii")
    @Operation(summary = "Get DII cash-market flows")
    public InstitutionalFlowResponse getDii(
            @RequestParam(defaultValue = "1D") String interval,
            @RequestParam(required = false) String from) {
        return marketInfoService.getDii(interval, from);
    }

    @GetMapping("/oi")
    @Operation(summary = "Get index option open interest")
    public OiResponse getOi(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "current_month") String expiry,
            @RequestParam(required = false) String date) {
        return marketInfoService.getOi(symbol, expiry, date);
    }

    @GetMapping("/change-oi")
    @Operation(summary = "Get change in index option open interest")
    public ChangeOiResponse getChangeOi(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "current_month") String expiry,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int interval) {
        return marketInfoService.getChangeOi(symbol, expiry, date, interval);
    }

    @GetMapping("/flows-overview")
    @Operation(summary = "Get FII, DII, and major-index OI overview")
    public FlowsOverviewResponse getFlowsOverview(
            @RequestParam(defaultValue = "1D") String interval) {
        return marketInfoService.getFlowsOverview(interval);
    }
}
