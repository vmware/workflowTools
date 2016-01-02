package com.vmware.http.exception;

import java.net.HttpURLConnection;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String errorText) {
        super(HttpURLConnection.HTTP_FORBIDDEN, errorText);
    }
}
