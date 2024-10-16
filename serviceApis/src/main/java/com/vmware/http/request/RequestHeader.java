package com.vmware.http.request;

import java.util.Base64;

import com.vmware.http.credentials.UsernamePasswordCredentials;

/**
 * Will be added as request headers to a request.
 */
public class RequestHeader extends RequestParam {

    public static final String AUTHORIZATION = "Authorization";

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
        String basicCredentials = Base64.getEncoder().encodeToString(credentials.toString().getBytes());
        return new RequestHeader(AUTHORIZATION, "Basic " + basicCredentials);
    }

    public static RequestHeader aBearerAuthHeader(String authValue) {
        return new RequestHeader(AUTHORIZATION, "Bearer " + authValue);
    }

    public static RequestHeader aTokenAuthHeader(String authValue) {
        return new RequestHeader(AUTHORIZATION, "token " + authValue);
    }

    public static RequestHeader aRefererHeader(String value) {
        return new RequestHeader("Referer", value);
    }

}
