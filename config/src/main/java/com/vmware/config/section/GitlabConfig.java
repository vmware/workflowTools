package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class GitlabConfig {

    @ConfigurableProperty(commandLine = "--gitlab-url", help = "Url for gitlab site")
    public String gitlabUrl;

    @ConfigurableProperty(commandLine = "--gitlab-project-id", help = "Id for gitlab project")
    public Integer gitlabProjectId;

    @ConfigurableProperty(commandLine = "--merge-branch-format", help = "Format for source branch for a merge request")
    public String gitlabMergeBranchFormat;

    @ConfigurableProperty(commandLine = "--source-merge-branch", help = "Specify custom source branch for a merge request")
    public String sourceMergeBranch;

    @ConfigurableProperty(commandLine = "--target-merge-branch", help = "Specify custom target branch for a merge request")
    public String targetMergeBranch;

}
