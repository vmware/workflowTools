package com.vmware.vcd.domain;

import com.vmware.http.HttpMethodType;

import java.util.Map;

public class ApiRequest {
    public HttpMethodType methodType;

    public String url;

    public String acceptType;

    public String contentType;

    public Map requestBody;
}
