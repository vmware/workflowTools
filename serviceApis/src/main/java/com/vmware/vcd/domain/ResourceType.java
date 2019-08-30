package com.vmware.vcd.domain;

import java.util.List;

public abstract class ResourceType {

    public String name;

    public String href;

    public List<LinkType> link;

    public LinkType getLinkByRel(String rel) {
        return link.stream().filter(linkValue -> linkValue.rel.equals(rel)).findFirst().orElse(null);
    }

    public LinkType getLinkByRelAndType(String rel, String type) {
        return link.stream().filter(linkValue -> linkValue.rel.equals(rel) && linkValue.type.equals(type)).findFirst().orElse(null);
    }
}
