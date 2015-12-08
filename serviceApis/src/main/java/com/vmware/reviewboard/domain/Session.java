package com.vmware.reviewboard.domain;

public class Session extends BaseEntity {

    public Link getUserLink() {
        return getLink("user");
    }
}
