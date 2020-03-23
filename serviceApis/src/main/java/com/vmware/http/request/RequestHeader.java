package com.vmware.http.request;

import javax.xml.bind.DatatypeConverter;

import com.vmware.http.credentials.UsernamePasswordCredentials;

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

    public static RequestHeader aContentTypeHeader(String value) {
        return new RequestHeader("Content-Type", value);
    }

    public static RequestHeader aBasicAuthHeader(UsernamePasswordCredentials credentials) {
        String basicCredentials = DatatypeConverter.printBase64Binary(credentials.toString().getBytes());
        return new RequestHeader("Authorization", "Basic " + basicCredentials);
    }

    public static RequestHeader aBearerAuthHeader(String authValue) {
        return new RequestHeader("Authorization", "Bearer " + authValue);
    }

    public static RequestHeader aRefererHeader(String value) {
        return new RequestHeader("Referer", value);
    }

}
