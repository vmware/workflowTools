/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class UriUtils {

    public static String buildUrl(String url, final RequestParam[] params) throws URISyntaxException, UnsupportedEncodingException {
        if (params == null) {
            return url;
        }
        boolean firstParamFound = false;
        for (int i = 0; i < params.length; i ++) {
            RequestParam param = params[i];
            if (!(param instanceof UrlParam)) {
                continue;
            }

            if (!firstParamFound) {
                url += "?";
                firstParamFound = true;
            }

            UrlParam urlParam = (UrlParam) param;
            url += urlParam.getName() + "=" + URLEncoder.encode(urlParam.getValue(), "UTF-8");
            if (i < params.length -1) {
                url += "&";
            }
        }
        return url;
    }
}
