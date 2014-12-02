/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class UriUtils {

    public static String buildUrl(String url, final RequestParam[] params) throws URISyntaxException, UnsupportedEncodingException {
        if (params == null) {
            return url;
        }

        List<UrlParam> urlParams = selectUrlParamsFromParams(params);

        if (urlParams.isEmpty()) {
            return url;
        }

        url = constructUrlWithParams(url, urlParams);
        return url;
    }

    private static String constructUrlWithParams(String url, List<UrlParam> urlParams) throws UnsupportedEncodingException {
        url += "?";

        for (int i = 0; i < urlParams.size(); i ++) {
            UrlParam urlParam = urlParams.get(i);
            url += urlParam.getName() + "=" + URLEncoder.encode(urlParam.getValue(), "UTF-8");
            if (i < urlParams.size() -1) {
                url += "&";
            }
        }

        return url;
    }

    private static List<UrlParam> selectUrlParamsFromParams(RequestParam[] params) {
        List<UrlParam> urlParams = new ArrayList<UrlParam>();

        for (RequestParam param : params) {
            if (param instanceof UrlParam) {
                urlParams.add((UrlParam) param);
            }
        }

        return urlParams;
    }
}
