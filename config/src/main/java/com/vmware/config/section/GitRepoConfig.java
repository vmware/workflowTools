package com.vmware.config.section;

import com.google.gson.annotations.Expose;
import com.vmware.config.CalculatedProperty;
import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;
import com.vmware.util.scm.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.TreeMap;

public class GitRepoConfig {

    @Expose(serialize = false, deserialize = false)
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Expose(serialize = false, deserialize = false)
    private final Git git = new Git();

    @ConfigurableProperty(commandLine = "-tb,--tracking-branch",
            help = "Tracking branch to use as base for reviews and for pushing commits. Combined with defaultGitRemote if no remote specified.",
            methodNameForValueCalculation = "determineTrackingBranchPath")
    public String trackingBranch;

    @ConfigurableProperty(commandLine = "-p,--parent", help = "Parent branch to use for the git diff to upload to review board. Combined with defaultGitRemote if no remote specified.")
    public String parentBranch;

    @ConfigurableProperty(commandLine = "-b,--branch", help = "Optional value to set if using the local branch name for review board is not desired")
    public String targetBranch;

    @ConfigurableProperty(help = "Default git remote. Remote used for pushing to master or other remote branches.")
    public String defaultGitRemote;

    @ConfigurableProperty(help = "Map of remote branches, $USERNAME is substituted for the real username.")
    public TreeMap<String, String> remoteBranches;

    @ConfigurableProperty(commandLine = "-rb,--remote-branch", help = "Remote branch name to use")
    public String remoteBranchToUse;

    @ConfigurableProperty(commandLine = "--use-git-tracking-branch", help = "Use git tracking branch as tracking branch for review and sandbox")
    public boolean useGitTrackingBranch;

    @ConfigurableProperty(commandLine = "--max-commits", help = "Max number of commits to check")
    public int maxCommitsToCheck;

    @ConfigurableProperty(commandLine = "--since-date", help = "Commits since date")
    public Date sinceDate;

    @ConfigurableProperty(commandLine = "--no-verify", help = "Skip running of pre-commit and commit-msg hooks by git")
    public boolean noVerify;

    @ConfigurableProperty(commandLine = "--no-precommit", help = "Skip running of pre-commit file by workflow tools setting commit details")
    public boolean noPreCommit;

    @ConfigurableProperty(commandLine = "--source-merge-branch", help = "Specify custom source branch for a request")
    public String sourceMergeBranch;

    @ConfigurableProperty(commandLine = "--target-merge-branch", help = "Specify custom target branch for a request")
    public String targetMergeBranch;

    @ConfigurableProperty(help = "Format for source branch for a request")
    public String gitMergeBranchFormat;

    @ConfigurableProperty(commandLine = "--mark-as-draft", help = "Whether to mark request as a draft")
    public boolean markAsDraft;

    @ConfigurableProperty(commandLine = "--fail-if-no-request-found", help = "Fail workflow if no request found")
    public boolean failIfNoRequestFound;

    public String trackingBranchPath() {
        return String.valueOf(determineTrackingBranchPath().getValue());
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
        log.debug("Local git branch {}", targetBranchValue);
        if (StringUtils.isNotEmpty(targetBranch)) {
            log.info("Overwriting branch property to {} (read from application config)", targetBranch);
            targetBranchValue = targetBranch;
        }
        return targetBranchValue;
    }

}
