package com.vmware.http.exception;

import java.net.HttpURLConnection;

public class MethodNotAllowedException extends ApiException {

    public MethodNotAllowedException(String errorText) {
        super(HttpURLConnection.HTTP_BAD_METHOD, errorText);
    }
}
