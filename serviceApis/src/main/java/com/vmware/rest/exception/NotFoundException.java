package com.vmware.rest.exception;

import java.net.HttpURLConnection;

public class NotFoundException extends ApiException {

    public NotFoundException(final String errorText) {
        super(HttpURLConnection.HTTP_NOT_FOUND, errorText);
    }
}
