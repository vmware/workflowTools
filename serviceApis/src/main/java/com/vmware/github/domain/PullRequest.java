package com.vmware.github.domain;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class PullRequest {
    public long id;
    public String nodeId;
    public long number;
    public String state;
    public boolean locked;
    public String title;
    public String body;
    public User user;
    public User[] requestedReviewers;
    public Team[] requestedTeams;
    public CommitRef head;
    public CommitRef base;
    public boolean draft;
    public Date createdAt;
    public Date mergedAt;
    public String htmlUrl;
    public String url;

    public String repoOwnerName() {
        return head != null ? head.repo.owner.login : null;
    }

    public String repoName() {
        return head != null ? head.repo.name : null;
    }

    public PullRequestForUpdate pullRequestForUpdate() {
        PullRequestForUpdate pullRequestForUpdate = new PullRequestForUpdate();
        pullRequestForUpdate.repoOwner = repoOwnerName();
        pullRequestForUpdate.repoName = repoName();
        pullRequestForUpdate.number = number;
        pullRequestForUpdate.state = state;
        pullRequestForUpdate.title = title;
        pullRequestForUpdate.body = body;
        pullRequestForUpdate.draft = draft;
        return pullRequestForUpdate;
    }
}
