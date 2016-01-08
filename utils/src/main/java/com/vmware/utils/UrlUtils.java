package com.vmware.utils;

/**
 * Util methods for url functionality.
 */
public class UrlUtils {

    public static String addTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url : url + "/";
    }
}
