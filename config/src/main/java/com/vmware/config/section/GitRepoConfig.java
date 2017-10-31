package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

import java.util.TreeMap;

public class GitRepoConfig {

    @ConfigurableProperty(commandLine = "-tb,--tracking-branch", help = "Tracking branch to use as base for reviews and for pushing commits. Combined with defaultGitRemote if no remote specified.")
    public String trackingBranch;

    @ConfigurableProperty(commandLine = "-p,--parent", help = "Parent branch to use for the git diff to upload to review board. Combined with defaultGitRemote if no remote specified.")
    public String parentBranch;

    @ConfigurableProperty(commandLine = "-b,--branch", help = "Optional value to set if using the local branch name for review board is not desired")
    public String targetBranch;

    @ConfigurableProperty(commandLine = "--git-remote", help = "Default git remote. Remote used for pushing to master or other remote branches.")
    public String defaultGitRemote;

    @ConfigurableProperty(help = "Map of remote branches, $USERNAME is substituted for the real username.")
    public TreeMap<String, String> remoteBranches;

    @ConfigurableProperty(commandLine = "-rb, --remote-branch", help = "Remote branch name to use")
    public String remoteBranchToUse;

    public String trackingBranchPath() {
        if (trackingBranch.contains("/")) {
            return trackingBranch;
        }
        return defaultGitRemote + "/" + trackingBranch;
    }

    public String parentBranchPath() {
        if (parentBranch.startsWith("/")) { // assuming local branch
            return parentBranch.substring(1);
        }
        // check if it has a slash or is a relative path
        if (parentBranch.contains("/") || parentBranch.toLowerCase().contains("head")) {
            return parentBranch;
        }
        return defaultGitRemote + "/" + parentBranch;
    }

}
