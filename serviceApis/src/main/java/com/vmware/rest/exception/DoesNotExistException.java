package com.vmware.rest.exception;

public class DoesNotExistException extends ApiException {

    public static final int STATUS_CODE = 100;

    public DoesNotExistException(String errorText) {
        super(STATUS_CODE, errorText);
    }
}
