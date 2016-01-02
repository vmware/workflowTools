package com.vmware.http.exception;

import java.net.HttpURLConnection;

public class BadRequestException extends ApiException {

    public BadRequestException(String errorText) {
        super(HttpURLConnection.HTTP_BAD_REQUEST, errorText);
    }
}
