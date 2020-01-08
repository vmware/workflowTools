package com.vmware.gitlab.domain;

import com.google.gson.annotations.Expose;

public class MergeRequest {
    public Integer id;

    public Integer iid;

    @Expose(deserialize = false)
    public int targetProjectId;

    @Expose(serialize = false)
    public int projectId;

    public String sourceBranch;

    public String targetBranch;

    public String title;

    @Expose(serialize = false)
    public String state;

    @Expose(deserialize = false)
    public String stateEvent;

    public boolean removeSourceBranch;

    public String webUrl;

    public boolean squash;
}
