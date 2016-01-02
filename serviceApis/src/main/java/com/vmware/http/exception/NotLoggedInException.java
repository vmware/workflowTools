package com.vmware.http.exception;

public class NotLoggedInException extends ApiException {

    public static final int STATUS_CODE = 103;

    public NotLoggedInException(String errorText) {
        super(STATUS_CODE, errorText);
    }
}
