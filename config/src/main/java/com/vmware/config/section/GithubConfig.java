package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class GithubConfig {

    @ConfigurableProperty(help = "Api url for github site")
    public String githubUrl;

    @ConfigurableProperty(help = "Api url for github site")
    public String githubGraphqlUrl;

    @ConfigurableProperty(help = "Github release url for workflow tools")
    public String workflowGithubReleasePath;

    @ConfigurableProperty(help = "Name for github repo owner")
    public String githubRepoOwnerName;

    @ConfigurableProperty(help = "Name for github repo")
    public String githubRepoName;

    @ConfigurableProperty(commandLine = "--merge-method", help = "Method to use for merging, can be merge, squash or rebase")
    public String mergeMethod;
}
