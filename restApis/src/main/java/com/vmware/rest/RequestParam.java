/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest;

/**
 * Used as a base class for UrlParam and RequestHeader.
 * The RestConnection class will handle params correctly depending on class type.
 */
public class RequestParam {
    protected String name;

    protected String value;

    protected RequestParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestParam param = (RequestParam) o;

        if (name != null ? !name.equals(param.name) : param.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
