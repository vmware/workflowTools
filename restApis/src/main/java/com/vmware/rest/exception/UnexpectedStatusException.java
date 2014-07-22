package com.vmware.rest.exception;

public class UnexpectedStatusException extends ApiException {

    public UnexpectedStatusException(int status, String errorText) {
        super(status, errorText);
    }
}
