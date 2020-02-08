package com.vmware.util;

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

    public static String removeTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String addRelativePaths(String url, String... paths) {
        if (paths.length == 0) {
            return url;
        }
        String urlWithoutTrailingSlash = removeTrailingSlash(url);
        StringBuilder urlBuilder = new StringBuilder(urlWithoutTrailingSlash);
        for (String path : paths) {
            if (path.startsWith("/")) {
                urlBuilder.append(path);
            } else {
                urlBuilder.append("/").append(path);
            }
        }
        return urlBuilder.toString();
    }
}
