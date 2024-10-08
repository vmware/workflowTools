package com.vmware.gitlab.domain;

import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MergeRequestApprovals {

    public boolean approved;

    public Integer approvalsRequired;

    public Integer approvalsLeft;

    public ApprovalUser[] approvedBy;

    public User[] suggestedApprovers;


    public boolean userHasApproved;

    public boolean userCanApprove;

    public String approvalInfo() {
        String approvalString = approvedBy == null ? "" :
                Arrays.stream(approvedBy).map(approvalUser -> approvalUser.user.name).collect(Collectors.joining(","));
        if (StringUtils.isNotBlank(approvalString)) {
            approvalString = ", already approved by: " + approvalString;
        }
        if (approvalsLeft == null || approvalsLeft == 0) {
            return "No approvals needed" + approvalString;
        }
        String suggestedApproversString = suggestedApprovers == null ? "" :
                Arrays.stream(suggestedApprovers).map(user -> user.name).collect(Collectors.joining(","));
        if (StringUtils.isNotBlank(suggestedApproversString)) {
            suggestedApproversString = ", suggested approvers: " + suggestedApproversString;
        }
        return StringUtils.pluralize(approvalsLeft, "approval") + " still needed" + approvalString + suggestedApproversString;
    }
}
