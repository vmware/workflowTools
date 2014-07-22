package com.vmware.reviewboard.domain;

public class RootList extends BaseEntity {

    public Link getReviewRequestsLink() {
        return getLink("review_requests");
    }

    public Link getInfoLink() {
        return getLink("info");
    }

    public Link getSessionLink() {
        return getLink("session");
    }
}
