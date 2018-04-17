package com.vmware.reviewboard.domain;

import java.util.Map;

import com.google.gson.annotations.Expose;

public class BaseEntity {
    @Expose(serialize = false)
    protected Map<String, Link> links;

    public Link getSelfLink() {
        return getLink("self");
    }

    public Link getUpdateLink() {
        return getLink("update");
    }


    public Link getDeleteLink() {
        return getLink("delete");
    }

    protected Link getLink(String name) {
        Link link = links.get(name);
        if (link == null) {
            return null;
        }
        return new Link(link);
    }

}
