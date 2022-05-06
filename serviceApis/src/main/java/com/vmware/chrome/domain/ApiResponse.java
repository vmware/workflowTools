package com.vmware.chrome.domain;

import java.util.Collections;
import java.util.Map;

public class ApiResponse {
    public int id;

    public String method;

    public Params params;

    public Result result;

    public Map exceptionDetails;

    public String getType() {
        return (String) getValueMap().get("type");
    }

    public String getDescrption() {
        return (String) getValueMap().get("description");
    }

    public String getValue() {
        return (String) getValueMap().get("value");
    }

    public String getData() {
        return result != null ? result.data : null;
    }

    public String getParamsData() {
        return params != null ? params.data : null;
    }

    public String getClassName() {
        return (String) getValueMap().get("className");
    }

    private Map getValueMap() {
        if (this.result == null || this.result.result == null) {
            return Collections.emptyMap();
        }
        return this.result.result;
    }

    public class Result {
        public Map result;

        private String data;
    }

    public class Params {

        private String data;
    }

}
