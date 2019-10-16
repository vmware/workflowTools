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

    public static String addRelativePaths(String url, String... paths) {
        if (paths.length == 0) {
            return url;
        }
        String urlWithTrailingSlash = addTrailingSlash(url);
        StringBuilder urlBuilder = new StringBuilder(urlWithTrailingSlash);
        for (String path : paths) {
            if (path.startsWith("/")) {
                urlBuilder.append(path.substring(1));
            } else {
                urlBuilder.append("/").append(path);
            }
        }
        return urlBuilder.toString();
    }
}
