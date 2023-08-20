package com.vmware.gitlab.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MergeRequestApprovalRule {

    public long id;

    public String name;

    @SerializedName("approvals_required")
    public int approvalsRequired;
    @Expose(deserialize = false)
    public String[] usernames;

    @SerializedName("group_ids")
    @Expose(deserialize = false)
    public long[] groupIds;

    @SerializedName("eligible_approvers")
    @Expose(serialize = false)
    public User[] eligibleApprovers;

    @Expose(serialize = false)
    public User[] users;

    @Expose(serialize = false)
    public Group[] groups;

}
