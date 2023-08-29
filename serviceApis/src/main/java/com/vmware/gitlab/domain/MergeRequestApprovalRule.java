package com.vmware.gitlab.domain;

import com.google.gson.annotations.Expose;

public class MergeRequestApprovalRule {

    public long id;

    public String name;

    public int approvalsRequired;
    @Expose(deserialize = false)
    public String[] usernames;

    @Expose(deserialize = false)
    public long[] groupIds;

    @Expose(serialize = false)
    public User[] eligibleApprovers;

    @Expose(serialize = false)
    public User[] users;

    @Expose(serialize = false)
    public Group[] groups;

}
