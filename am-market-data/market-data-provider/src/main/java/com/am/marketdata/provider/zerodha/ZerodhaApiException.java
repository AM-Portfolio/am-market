package com.am.marketdata.provider.zerodha;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * Custom exception for Zerodha API operations
 * Provides additional context and error handling for Zerodha API calls
 */
public class ZerodhaApiException extends RuntimeException {

    private int errorCode;
    private String errorType;

    /**
     * Creates a new ZerodhaApiException with a message
     * 
     * @param message Error message
     */
    public ZerodhaApiException(String message) {
        super(message);
        this.errorType = "unknown";
    }

    /**
     * Creates a new ZerodhaApiException with a message and cause
     * 
     * @param message Error message
     * @param cause   Original exception
     */
    public ZerodhaApiException(String message, Throwable cause) {
        super(message, cause);

        if (cause instanceof KiteException) {
            KiteException kiteException = (KiteException) cause;
            this.errorCode = kiteException.code;

            if (kiteException.code == 403) {
                this.errorType = "unauthorized";
            } else if (kiteException.code >= 400 && kiteException.code < 500) {
                this.errorType = "client_error";
            } else if (kiteException.code >= 500) {
                this.errorType = "server_error";
            } else {
                this.errorType = "kite_error";
            }
        } else {
            this.errorType = cause instanceof java.io.IOException ? "network_error" : "unexpected_error";
        }
    }

    /**
     * Creates a new ZerodhaApiException with a message, cause, and error code
     * 
     * @param message   Error message
     * @param cause     Original exception
     * @param errorCode HTTP error code
     */
    public ZerodhaApiException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;

        if (errorCode == 403) {
            this.errorType = "unauthorized";
        } else if (errorCode >= 400 && errorCode < 500) {
            this.errorType = "client_error";
        } else if (errorCode >= 500) {
            this.errorType = "server_error";
        } else {
            this.errorType = "unknown";
        }
    }

    /**
     * Get the error code
     * 
     * @return HTTP error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Get the error type
     * 
     * @return Error type string
     */
    public String getErrorType() {
        return errorType;
    }
}
