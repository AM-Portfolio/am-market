package com.am.marketdata.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Data API Documentation")
                        .description("API documentation for the Market Data service providing access to stock quotes, " +
                                "historical data, market indices, brokerage calculations, and more.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AM Portfolio Team")
                                .email("support@amportfolio.com"))
                        .license(new License()
                                .name("Private License")
                                .url("https://amportfolio.com/license")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Default Server URL")
                ));
    }
}
