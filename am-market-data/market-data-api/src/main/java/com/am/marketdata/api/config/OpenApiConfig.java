package com.am.marketdata.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI 3.0 documentation in am-market-data.
 * Adheres to Clean Architecture standards with centralized security schemes and tags.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Data Microservice API Documentation")
                        .description("High-performance Market Data API service providing real-time stock quotes, " +
                                "OHLC candle data, market index streams, SPaN margin calculations, and security explorer fuzzy search.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AM Portfolio SRE & Core Dev Team")
                                .email("support@asrax.in"))
                        .license(new License()
                                .name("Private Enterprise License")
                                .url("https://asrax.in/license")))
                .servers(List.of(
                        new Server()
                                .url("/market")
                                .description("Development / Production Ingress Base Path"),
                        new Server()
                                .url("/")
                                .description("Direct Local Container / Service Port")
                ))
                .tags(List.of(
                        new Tag().name("Market Data").description("Real-time quotes, OHLC tick candles, and live price math"),
                        new Tag().name("Securities Search").description("Security explorer search and batch lookup endpoints"),
                        new Tag().name("Margin Calculator").description("SPaN and exposure margin calculation engine"),
                        new Tag().name("Indices").description("Market indices data and batch scrapers"),
                        new Tag().name("Brokerage").description("Brokerage fee and exchange tax calculations")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")));
    }
}
