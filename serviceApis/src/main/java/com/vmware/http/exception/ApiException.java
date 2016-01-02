package com.vmware.http.exception;

public class ApiException extends RuntimeException {

    protected ApiException(final int status, final String errorText) {
        super("Status " + status + " Message: " + errorText);
    }
}
