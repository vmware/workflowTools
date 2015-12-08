package com.vmware.reviewboard.domain;

public class ReviewRequests extends BaseEntity {

    public int total_results;

    public ReviewRequest[] review_requests;

    public Link getCreateLink() {
        return getLink("create");
    }

}
