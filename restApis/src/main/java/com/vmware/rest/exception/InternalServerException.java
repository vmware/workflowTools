package com.vmware.rest.exception;

import java.net.HttpURLConnection;

public class InternalServerException extends ApiException {

    public InternalServerException(final String errorText) {
        super(HttpURLConnection.HTTP_INTERNAL_ERROR, errorText);
    }
}
