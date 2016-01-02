package com.vmware.http.exception;

public class UnexpectedStatusException extends ApiException {

    public UnexpectedStatusException(int status, String errorText) {
        super(status, errorText);
    }
}
