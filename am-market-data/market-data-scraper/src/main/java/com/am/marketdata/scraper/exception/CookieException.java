package com.am.marketdata.scraper.exception;

public class CookieException extends MarketDataException {
    public CookieException(String message) {
        super(message);
    }

    public CookieException(String message, Throwable cause) {
        super(message, cause);
    }
}
