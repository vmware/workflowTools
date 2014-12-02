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
    private String name;

    private String value;

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
}
