package com.vmware.gitlab.domain;

public class MergeAcceptRequest {

    public MergeAcceptRequest(MergeRequest mergeRequest) {
        this.sha = mergeRequest.sha;
        this.mergeWhenPipelineSucceeds = true;
        this.shouldRemoveSourceBranch = mergeRequest.forceRemoveSourceBranch;
        this.squash = mergeRequest.squash;
    }

    public String sha;

    public boolean mergeWhenPipelineSucceeds;

    public boolean shouldRemoveSourceBranch;

    public boolean squash;
}
