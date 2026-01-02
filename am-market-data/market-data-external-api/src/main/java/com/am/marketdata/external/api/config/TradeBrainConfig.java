package com.am.marketdata.external.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Data
@Configuration
@Primary
@ConfigurationProperties(prefix = "trade-brain")
public class TradeBrainConfig {
    private String baseUrl;
    private Api api;
    private Headers headers;

    @Data
    public static class Api {
        private Company company;
        private Indices indices;
    }

    @Data
    public static class Company {
        private Profile profile;
        private Financial financial;
        private Documents documents;
        private Reports reports;
        private Stock stock;
        private Analytics analytics;
    }

    @Data
    public static class Analytics {
        private String analytics;
    }

    @Data
    public static class Profile {
        private String shortDetails;
        private String companyProfile;
        private String companyInsights;
        private String keymetrics;
        private String researchReport;
        private String corporateActions;
        private String topShareholders;
        private String companyScore;
        private String boardOfDirectors;
    }

    @Data
    public static class Financial {
        private String dividends;
        private String halfYearly;
        private String quarterlyResults;
        private String profitAndLoss;
        private String balanceSheet;
        private String cashFlow;
    }

    @Data
    public static class Documents {
        private String creditRating;
        private String industryPeers;
    }

    @Data
    public static class Reports {
        private String directorReport;
        private String chairmanReport;
        private String auditorReport;
        private String annualReports;
    }

    @Data
    public static class Stock {
        private String details;
        private String historical;
        private String technical;
        private String shareholding;
        private String heatmap;
    }

    @Data
    public static class Indices {
        private String all;
    }

    @Data
    public static class Headers {
        private String userAgent;
        private String accept;
        private String acceptLanguage;
    }
}
