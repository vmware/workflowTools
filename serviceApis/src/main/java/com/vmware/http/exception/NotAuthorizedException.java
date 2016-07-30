package com.vmware.http.exception;

import java.net.HttpURLConnection;

public class NotAuthorizedException extends ApiException {

    public NotAuthorizedException(String errorText) {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, errorText);
    }

    public NotAuthorizedException(String errorText, Throwable cause) {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, errorText, cause);
    }
}
