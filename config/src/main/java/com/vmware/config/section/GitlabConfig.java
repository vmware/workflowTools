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

    @ConfigurableProperty(commandLine = "--merge-bot-user-id", help = "User if of the bot used for merging merge requests")
    public Integer mergeBotUserId;
}