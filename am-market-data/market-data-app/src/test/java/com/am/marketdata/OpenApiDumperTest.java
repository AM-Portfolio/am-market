package com.am.marketdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiDumperTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.api.service.MarketDataFetchService marketDataFetchService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.internal.service.MarketDataHistoricalSyncService marketDataHistoricalSyncService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.internal.service.MarketDataIngestionService marketDataIngestionService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.service.margin.BrokerageCalculatorService brokerageCalculatorService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.service.margin.MarginCalculatorService marginCalculatorService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.api.service.MarketAnalyticsService marketAnalyticsService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.api.service.StockIndicesService stockIndicesService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.am.marketdata.internal.repository.IngestionJobLogRepository ingestionJobLogRepository;

    @Test
    public void dumpOpenApiSpec() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Determine output path: am-market-sdk/market-data-openapi.json
        // Working dir is usually module root (am-market-data/market-data-app)
        // We want to go up to am-market/am-market-sdk
        Path outputDir = Paths.get("../../am-market-sdk");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Path outputFile = outputDir.resolve("market-data-openapi.json");

        try (PrintWriter out = new PrintWriter(outputFile.toFile())) {
            out.println(spec);
        }

        System.out.println("Extracted OpenAPI spec to: " + outputFile.toAbsolutePath());
    }
}
