package com.vmware.gitlab.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MergeRequestApprovals {

    public boolean approved;

    public Integer approvalsRequired;

    public Integer approvalsLeft;

    public ApprovalUser[] approvedBy;

    @SerializedName("suggested_approvers")
    public User[] suggestedApprovers;


    public boolean userHasApproved;

    public boolean userCanApprove;

    public String approvalInfo() {
        String approvalString = approvedBy == null ? "" :
                Arrays.stream(approvedBy).map(approvalUser -> approvalUser.user.username).collect(Collectors.joining(","));
        if (StringUtils.isNotBlank(approvalString)) {
            approvalString = ", already approved by: " + approvalString;
        }
        if (approvalsLeft == null || approvalsLeft == 0) {
            return "No approvals needed" + approvalString;
        }
        String suggestedApproversString = suggestedApprovers == null ? "" :
                Arrays.stream(suggestedApprovers).map(user -> user.username).collect(Collectors.joining(","));
        if (StringUtils.isNotBlank(suggestedApproversString)) {
            suggestedApproversString = ", suggested approvers: " + suggestedApproversString;
        }
        return StringUtils.pluralize(approvalsLeft, "approval") + " still needed" + approvalString + suggestedApproversString;
    }
}
