package com.vmware.rest.exception;

import java.net.HttpURLConnection;

public class NotAuthorizedException extends ApiException {

    public NotAuthorizedException(String errorText) {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, errorText);
    }
}
