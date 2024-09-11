package com.vmware.github.domain;

import java.util.Date;

public class Review {
    public User user;
    public String id;
    public String htmlUrl;
    public String body;
    public String state;
    public String pullRequestUrl;
    public Date submittedAt;
    public String commitId;
    public String authorAssociation;

    public boolean isApproved() {
        return "APPROVED".equals(state);
    }
}
