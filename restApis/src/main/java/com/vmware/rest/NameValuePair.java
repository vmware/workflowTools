/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest;

public class NameValuePair {
    private String name;

    private String value;

    public NameValuePair(String name, String value) {
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
