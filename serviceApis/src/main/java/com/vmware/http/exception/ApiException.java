package com.vmware.http.exception;

public abstract class ApiException extends RuntimeException {

    private final int statusCode;

    protected ApiException(final int status, final String errorText) {
        super(createReadableErrorMessage(status, errorText));
        this.statusCode = status;
    }

    protected ApiException(final int status, final String errorText, Throwable throwable) {
        super(createReadableErrorMessage(status, errorText), throwable);
        this.statusCode = status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    private static String createReadableErrorMessage(int status, String errorText) {
        if (errorText == null) {
            return "Status " + status;
        } else {
            return "Status " + status + " " + errorText.replace("\\n\\t", "\n   ");
        }
    }

}
