package com.vmware.http.exception;

public class PermissionDeniedException extends ApiException {

    public static final int STATUS_CODE = 101;

    public PermissionDeniedException(String errorText) {
        super(STATUS_CODE, errorText);
    }
}
