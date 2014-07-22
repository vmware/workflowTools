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

    public static String buildUrl(String url, final NameValuePair[] params) throws URISyntaxException, UnsupportedEncodingException {
        if (params == null || params.length == 0) {
            return url;
        }
        url += "?";
        for (int i = 0; i < params.length; i ++) {
            NameValuePair param = params[i];
            url += param.getName() + "=" + URLEncoder.encode(param.getValue(), "UTF-8");
            if (i < params.length -1) {
                url += "&";
            }
        }
        return url;
    }
}
