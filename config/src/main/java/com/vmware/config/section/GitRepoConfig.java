package com.vmware.config.section;

import com.vmware.config.CalculatedProperty;
import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;
import com.vmware.util.scm.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;

public class GitRepoConfig {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @ConfigurableProperty(commandLine = "-tb,--tracking-branch",
            help = "Tracking branch to use as base for reviews and for pushing commits. Combined with defaultGitRemote if no remote specified.",
            methodNameForValueCalculation = "determineTrackingBranchPath")
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

    @ConfigurableProperty(commandLine = "--use-git-tracking-branch", help = "Use git tracking branch as tracking branch for review")
    public boolean useGitTrackingBranch;

    public String trackingBranchPath() {
        return determineTrackingBranchPath().getValue();
    }

    public CalculatedProperty determineTrackingBranchPath() {
        if (useGitTrackingBranch) {
            String gitTrackingBranch = getGitTrackingBranch();
            if (gitTrackingBranch != null) {
                return new CalculatedProperty(gitTrackingBranch, "git tracking branch");
            }
        }
        if (trackingBranch.contains("/")) {
            return new CalculatedProperty(trackingBranch, "trackingBranch");
        }
        return new CalculatedProperty(defaultGitRemote + "/" + trackingBranch, "trackingBranch");
    }

    private String getGitTrackingBranch() {
        Git git = new Git();
        if (git.workingDirectoryIsInGitRepo()) {
            return git.getTrackingBranch();
        } else {
            return null;
        }
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

    public String determineBranchName() {
        Git git = new Git();
        if (!git.workingDirectoryIsInGitRepo()) {
            return "";
        }
        String targetBranchValue = git.currentBranch();
        log.debug("Using local git branch {}", targetBranchValue);
        if (StringUtils.isNotBlank(targetBranch)) {
            log.info("Setting branch property to {} (read from application config)", targetBranch);
            targetBranchValue = targetBranch;
        }
        return targetBranchValue;
    }

}
