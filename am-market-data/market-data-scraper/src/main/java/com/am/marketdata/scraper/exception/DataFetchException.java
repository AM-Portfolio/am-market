package com.am.marketdata.scraper.exception;

public class DataFetchException extends MarketDataException {
    private final String dataType;
    private final int retryCount;

    public DataFetchException(String dataType, int retryCount, String message) {
        super(message);
        this.dataType = dataType;
        this.retryCount = retryCount;
    }

    public DataFetchException(String dataType, int retryCount, String message, Throwable cause) {
        super(message, cause);
        this.dataType = dataType;
        this.retryCount = retryCount;
    }

    public String getDataType() {
        return dataType;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
