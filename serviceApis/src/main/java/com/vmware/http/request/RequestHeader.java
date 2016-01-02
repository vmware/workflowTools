package com.vmware.http.request;

/**
 * Will be added as request headers to a request.
 */
public class RequestHeader extends RequestParam {

    public RequestHeader(String name, String value) {
        super(name, value);
    }

    public static RequestHeader anAcceptHeader(String value) {
        return new RequestHeader("Accept", value);
    }


}
