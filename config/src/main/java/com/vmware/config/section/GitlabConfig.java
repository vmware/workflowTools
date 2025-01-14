package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class GitlabConfig {

    @ConfigurableProperty(help = "Url for gitlab site")
    public String gitlabUrl;

    @ConfigurableProperty(help = "Id for gitlab project")
    public Integer gitlabProjectId;

    @ConfigurableProperty(help = "User if of the bot used for merging merge requests")
    public Integer mergeBotUserId;

    @ConfigurableProperty(commandLine = "--approval-rule-name", help = "Name of the approval rule to add reviewers to")
    public String approvalRuleName;

    @ConfigurableProperty(commandLine = "--approvals-required", help = "Number of approvals required for the merge request")
    public int approvalsRequired;

    @ConfigurableProperty(commandLine = "--allow-self-approval", help = "Allows self approval for a merge request")
    public boolean allowSelfApproval;

    @ConfigurableProperty(help = "Prefix for draft merge request")
    public String draftMergeRequestPrefix;

    @ConfigurableProperty(commandLine = "--gitlab-username", help = "Username for gitlab if not the default username")
    public String gitlabUsername;
}