package com.vmware.http;

import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.UrlParam;
import com.vmware.utils.StringUtils;
import com.vmware.utils.collections.OverwritableSet;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Encapsulates a collection of request parameters.
 */
public class RequestParams {

    private Collection<RequestParam> statefulParams = new HashSet<>();

    private Collection<RequestParam> allParams = new OverwritableSet<>();


    public String buildUrl(String url) throws URISyntaxException, UnsupportedEncodingException {
        if (allParams.isEmpty()) {
            return url;
        }

        List<UrlParam> urlParams = urlParams();

        if (urlParams.isEmpty()) {
            return url;
        }

        url = constructUrlWithParams(url, urlParams);
        return url;
    }

    public void addStatefulParam(RequestParam requestParam) {
        statefulParams.add(requestParam);
        allParams.add(requestParam);
    }

    public void addStatelessParam(RequestParam requestParam) {
        statefulParams.add(requestParam);
        allParams.add(requestParam);
    }

    public void addAllStatefulParams(Collection<? extends RequestParam> requestParams) {
        statefulParams.addAll(requestParams);
        allParams.addAll(requestParams);
    }

    public void addAllStatelessParams(Collection<? extends RequestParam> requestParams) {
        allParams.addAll(requestParams);
    }

    public void addStatefulParamsFromUrlFragment(String urlFragment) {
        if (StringUtils.isBlank(urlFragment)) {
            return;
        }

        for (String paramText : urlFragment.split("&")) {
            allParams.add(UrlParam.fromText(paramText));
        }
    }

    public void reset() {
        statefulParams.clear();
        allParams.clear();
    }

    public void clearStatelessParams() {
        allParams.retainAll(statefulParams);
    }

    public List<UrlParam> urlParams() {
        List<UrlParam> urlParams = new ArrayList<>();

        for (RequestParam param : allParams) {
            if (param instanceof UrlParam) {
                urlParams.add((UrlParam) param);
            }
        }

        return urlParams;
    }

    public List<RequestHeader> requestHeaders() {
        List<RequestHeader> requestHeaders = new ArrayList<>();

        for (RequestParam param : allParams) {
            if (param instanceof RequestHeader) {
                requestHeaders.add((RequestHeader) param);
            }
        }

        return requestHeaders;
    }

    private String constructUrlWithParams(String url, List<UrlParam> urlParams) throws UnsupportedEncodingException {
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

}
