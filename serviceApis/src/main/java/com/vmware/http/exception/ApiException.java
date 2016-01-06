package com.vmware.http.exception;

public class ApiException extends RuntimeException {

    protected ApiException(final int status, final String errorText) {
        super("Status " + status + " Message: " + errorText);
    }

    protected ApiException(final int status, final String errorText, Throwable throwable) {
        super("Status " + status + " Message: " + errorText, throwable);
    }

}
