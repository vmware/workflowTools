package com.vmware.rest;

/**
 * Will be appended to the url as GET parameters for a request.
 */
public class UrlParam extends RequestParam {

    public UrlParam(String name, String value) {
        super(name, value);
    }
}
