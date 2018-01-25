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
        for (String path : paths) {
            if (!url.endsWith("/") && path.startsWith("/")) {
                url += path;
            } else if (url.endsWith("/") && path.startsWith("/")) {
                url += path.substring(1);
            } else {
                url += "/" + path;
            }
        }

        return url;
    }
}
