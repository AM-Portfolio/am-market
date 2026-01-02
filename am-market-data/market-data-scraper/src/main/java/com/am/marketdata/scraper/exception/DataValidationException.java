package com.am.marketdata.scraper.exception;

public class DataValidationException extends MarketDataException {
    private final String dataType;
    private final String validationError;

    public DataValidationException(String dataType, String validationError) {
        super(String.format("Validation failed for %s: %s", dataType, validationError));
        this.dataType = dataType;
        this.validationError = validationError;
    }

    public DataValidationException(String dataType, String validationError, Throwable cause) {
        super(String.format("Validation failed for %s: %s", dataType, validationError), cause);
        this.dataType = dataType;
        this.validationError = validationError;
    }

    public String getDataType() {
        return dataType;
    }

    public String getValidationError() {
        return validationError;
    }
}
