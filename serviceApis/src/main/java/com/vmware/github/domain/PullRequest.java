package com.vmware.github.domain;
import java.util.Date;

public class PullRequest {
    public long id;
    public long number;
    public String state;
    public boolean locked;
    public String title;
    public String body;
    public User user;
    public User[] requestedReviewers;
    public String repoOwner;
    public CommitRef head;
    public CommitRef base;
    public boolean draft;
    public Date createdAt;
    public Date mergedAt;
    public String htmlUrl;

    public String headRepo() {
        return head != null ? head.repoName() : null;
    }

    public PullRequestForUpdate pullRequestForUpdate() {
        PullRequestForUpdate pullRequestForUpdate = new PullRequestForUpdate();
        pullRequestForUpdate.number = number;
        pullRequestForUpdate.state = state;
        pullRequestForUpdate.title = title;
        pullRequestForUpdate.body = body;
        pullRequestForUpdate.draft = draft;
        pullRequestForUpdate.headRepo = head.repoName();
        pullRequestForUpdate.head = head.ref;
        pullRequestForUpdate.base = head.ref;
        return pullRequestForUpdate;
    }
}
