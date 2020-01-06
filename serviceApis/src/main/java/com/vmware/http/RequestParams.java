package com.vmware.http;

import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.UrlParam;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.OverwritableSet;
import com.vmware.util.exception.RuntimeIOException;

import java.io.UnsupportedEncodingException;
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


    public String buildUrl(String url) {
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
        statefulParams.removeIf(param -> param.getName().equals(requestParam.getName()));
        statefulParams.add(requestParam);
        allParams.removeIf(param -> param.getName().equals(requestParam.getName()));
        allParams.add(requestParam);
    }

    public void removeStatefulParam(String paramName) {
        statefulParams.removeIf(param -> param.getName().equalsIgnoreCase(paramName));
    }

    public void addStatelessParam(RequestParam requestParam) {
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
        if (StringUtils.isEmpty(urlFragment)) {
            return;
        }

        for (String paramText : urlFragment.split("&")) {
            addStatefulParam(UrlParam.fromText(paramText));
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

    private String constructUrlWithParams(String url, List<UrlParam> urlParams)  {
        url += "?";

        for (int i = 0; i < urlParams.size(); i ++) {
            UrlParam urlParam = urlParams.get(i);
            try {
                url += urlParam.getName() + "=" + URLEncoder.encode(urlParam.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeIOException(e);
            }
            if (i < urlParams.size() -1) {
                url += "&";
            }
        }

        return url;
    }

}
