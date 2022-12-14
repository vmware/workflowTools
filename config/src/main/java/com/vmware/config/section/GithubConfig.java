package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class GithubConfig {

    @ConfigurableProperty(help = "Api url for github site")
    public String githubUrl;

    @ConfigurableProperty(help = "Github release url for workflow tools")
    public String workflowGithubReleasePath;
}
