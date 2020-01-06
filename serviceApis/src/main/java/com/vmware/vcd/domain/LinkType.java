package com.vmware.vcd.domain;

public class LinkType {
    public String name;

    public String rel;

    public String type;

    public String href;

    public String model;

    public LinkType() {}

    public LinkType(String href) {
        this.href = href;
    }
}
