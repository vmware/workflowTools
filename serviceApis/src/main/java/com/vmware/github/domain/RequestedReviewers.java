package com.vmware.github.domain;

import java.util.Collection;

public class RequestedReviewers {
    public String[] reviewers;
    public String[] teamReviewers;

    public RequestedReviewers() {
    }

    public RequestedReviewers(Collection<User> users) {
        this.reviewers = users.stream().map(user -> user.login).toArray(String[]::new);
    }
}
