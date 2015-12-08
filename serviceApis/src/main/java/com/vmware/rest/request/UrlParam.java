package com.vmware.rest.request;

/**
 * Will be appended to the url as GET parameters for a request.
 */
public class UrlParam extends RequestParam {

    public UrlParam(String name, String value) {
        super(name, value);
    }

    public static UrlParam fromText(String paramText) {
        String[] paramPieces = paramText.split("=");
        if (paramPieces.length != 2) {
            throw new IllegalArgumentException(paramText + " is not a valid url parameter, rerun with -d flag for debugging info");
        }
        return new UrlParam(paramPieces[0], paramPieces[1]);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
