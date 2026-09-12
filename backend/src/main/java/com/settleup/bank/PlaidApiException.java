package com.settleup.bank;

public class PlaidApiException extends RuntimeException {

    private final String errorCode;

    public PlaidApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
