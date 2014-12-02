package com.vmware.rest;

/**
 * Will be added as request headers to a request.
 */
public class RequestHeader extends RequestParam {

    public RequestHeader(String name, String value) {
        super(name, value);
    }
}
