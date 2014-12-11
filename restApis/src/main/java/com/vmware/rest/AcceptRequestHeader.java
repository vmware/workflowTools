package com.vmware.rest;

public class AcceptRequestHeader extends RequestHeader {

    public static final String HEADER_NAME = "Accept";

    public AcceptRequestHeader(String value) {
        super(HEADER_NAME, value);
    }
}
