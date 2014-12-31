package com.vmware.rest;

import com.vmware.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UrlUtils {

    public static String buildUrl(String url, final Collection<? extends RequestParam> params) throws URISyntaxException, UnsupportedEncodingException {
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

    private static List<UrlParam> selectUrlParamsFromParams(Collection<? extends RequestParam> params) {
        List<UrlParam> urlParams = new ArrayList<UrlParam>();

        for (RequestParam param : params) {
            if (param instanceof UrlParam) {
                urlParams.add((UrlParam) param);
            }
        }

        return urlParams;
    }

    public static List<UrlParam> parseParamsFromText(String apiText) {
        List<UrlParam> params = new ArrayList<UrlParam>();
        if (StringUtils.isBlank(apiText)) {
            return params;
        }

        for (String paramText : apiText.split("&")) {
           params.add(UrlParam.fromText(paramText));
        }
        return params;
    }

    public static String convertParamsToText(List<UrlParam> params) {
        String paramText = "";

        for (UrlParam param : params) {
            if (!paramText.isEmpty()) {
                paramText += "&";
            }
            paramText += param.toString();
        }

        return paramText;
    }

    public static String addTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url : url + "/";
    }
}
