package com.vmware.gitlab.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MergeRequest {
    public Integer id;

    public Integer iid;

    public User author;

    @Expose(deserialize = false)
    public int targetProjectId;

    @Expose(serialize = false)
    public int projectId;

    public String sourceBranch;

    public String targetBranch;

    public String title;

    public String description;

    public String sha;

    public String mergeStatus;

    @Expose(serialize = false)
    public String state;

    @Expose(deserialize = false)
    public String stateEvent;

    @Expose(deserialize = false)
    public Boolean removeSourceBranch;

    public Boolean forceRemoveSourceBranch;

    public String webUrl;

    public boolean squash;

    public Integer assigneeId;

    public ApprovalUser[] appovedBy;

    @SerializedName("diff_refs")
    public DiffRefs diffRefs;

    @SerializedName("reviewer_ids")
    @Expose(deserialize = false)
    public long[] reviewerIds;

    public boolean canBeMerged() {
        return "can_be_merged".equalsIgnoreCase(mergeStatus);
    }

    public class DiffRefs {
        @SerializedName("base_sha")
        public String baseSha;
        @SerializedName("head_sha")
        public String headSha;
        @SerializedName("start_sha")
        public String startSha;

    }
}