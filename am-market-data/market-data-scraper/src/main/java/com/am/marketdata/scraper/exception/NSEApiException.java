package com.am.marketdata.scraper.exception;

import org.springframework.http.HttpStatusCode;

public class NSEApiException extends MarketDataException {
    private final String endpoint;
    private final HttpStatusCode statusCode;
    private final String responseBody;

    public NSEApiException(String endpoint, HttpStatusCode statusCode, String responseBody, String message) {
        super(String.format("NSE API error [%s] %s - %s: %s", endpoint, statusCode, message, responseBody));
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public NSEApiException(String endpoint, HttpStatusCode statusCode, String responseBody, String message, Throwable cause) {
        super(String.format("NSE API error [%s] %s - %s: %s", endpoint, statusCode, message, responseBody), cause);
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
