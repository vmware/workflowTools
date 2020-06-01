package com.vmware.gitlab.domain;

public class MergeRequestApprovals {

    public boolean approved;

    public Integer approvalsRequired;

    public Integer approvalsLeft;

    public ApprovalUser[] approvedBy;

    public boolean userHasApproved;

    public boolean userCanApprove;
}
