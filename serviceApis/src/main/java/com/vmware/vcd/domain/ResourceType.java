package com.vmware.vcd.domain;

import java.util.List;

import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

public abstract class ResourceType {

    public String name;

    public String href;

    public List<LinkType> link;

    public LinkType getSelfLink() {
        return getLinkByRel("self");
    }

    public LinkType getLinkByRel(String rel) {
        return link.stream().filter(linkValue -> linkValue.rel.equals(rel)).findFirst()
                .orElseThrow(() -> new FatalException("Failed to find link named " + rel));
    }

    public LinkType getLinkByRelAndType(String rel, String type) {
        if (link == null) {
            throw new FatalException("No links set for {}", name);
        }
        return link.stream().filter(linkValue -> linkValue.rel.equals(rel) && linkValue.type.equals(type)).findFirst()
                .orElseThrow(() -> new FatalException("No link with relation {} and type {} found for {}", rel, type, name));
    }

    public String parseIdFromRef() {
        if (StringUtils.isEmpty(href)) {
            return null;
        }
        String vappId = href.substring(href.lastIndexOf("/") + 1);
        if (vappId.startsWith("vapp-")) {
            vappId = vappId.substring("vapp-".length());
        }
        return vappId;
    }
}
