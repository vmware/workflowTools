package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class GitlabConfig {

    @ConfigurableProperty(help = "Url for gitlab site")
    public String gitlabUrl;

    @ConfigurableProperty(help = "Id for gitlab project")
    public Integer gitlabProjectId;

    @ConfigurableProperty(help = "Format for source branch for a merge request")
    public String gitlabMergeBranchFormat;

    @ConfigurableProperty(commandLine = "--source-merge-branch", help = "Specify custom source branch for a merge request")
    public String sourceMergeBranch;

    @ConfigurableProperty(commandLine = "--target-merge-branch", help = "Specify custom target branch for a merge request")
    public String targetMergeBranch;

    @ConfigurableProperty(help = "User if of the bot used for merging merge requests")
    public Integer mergeBotUserId;

    @ConfigurableProperty(commandLine = "--approval-rule-name", help = "Name of the approval rule to add reviewers to")
    public String approvalRuleName;

    @ConfigurableProperty(commandLine = "--approvals-required", help = "Number of approvals required for the merge request")
    public int approvalsRequired;
}