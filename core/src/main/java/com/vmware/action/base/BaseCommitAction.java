/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import java.util.Collections;
import java.util.List;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.NoPerforceClientForDirectoryException;
import com.vmware.util.scm.Perforce;

public abstract class BaseCommitAction extends BaseAction {

    protected ReviewRequestDraft draft;

    public BaseCommitAction(WorkflowConfig config) {
        super(config);
    }

    public void setDraft(ReviewRequestDraft draft) {
        this.draft = draft;
    }

    protected String determineChangelistIdToUse() {
        if (StringUtils.isNotEmpty(draft.perforceChangelistId)) {
            return draft.perforceChangelistId;
        } else if (StringUtils.isNotEmpty(perforceClientConfig.changelistId)) {
            return perforceClientConfig.changelistId;
        } else {
            return serviceLocator.getPerforce().selectPendingChangelist();
        }
    }

    protected void failIfGitRepoOrPerforceClientCannotBeUsed() {
        String gitReason = "", perforceReason = "";
        if (git.workingDirectoryIsInGitRepo()) {
            return; // don't need to check perforce
        } else {
            gitReason = "not in git repo";
        }
        perforceReason = reasonPerforceClientCannotBeUsed();
        if (StringUtils.isNotEmpty(gitReason) && StringUtils.isNotEmpty(perforceReason)) {
            exitDueToFailureCheck(gitReason + " and " + perforceReason);
        }
    }

    protected String readLastChange() {
        if (git.workingDirectoryIsInGitRepo()) {
            return git.lastCommitText();
        } else if (reasonPerforceClientCannotBeUsed() == null) {
            return readPendingChangelistText();
        } else {
            log.warn("Not in git repo and config value perforceClientName is not set, can't read last change");
            return "";
        }
    }

    protected List<FileChange> getFileChangesInLastCommit() {
        if (git.workingDirectoryIsInGitRepo()) {
            return git.getChangesInDiff("head~1", "head");
        } else if (reasonPerforceClientCannotBeUsed() == null) {
            String changelistId = determineChangelistIdToUse();
            return serviceLocator.getPerforce().getFileChangesForPendingChangelist(changelistId);
        } else {
            log.warn("Not in git repo and config value perforceClientName is not set, can't read last change");
            return Collections.emptyList();
        }
    }

    protected boolean commitTextHasChanges(boolean includeJobResultsInCommit) {
        ReviewRequestDraft existingDraft = new ReviewRequestDraft(readLastChange(), commitConfig);
        String existingCommitText = existingDraft.toText(commitConfig);
        String updatedCommitText = updatedCommitText(includeJobResultsInCommit);

        return !existingCommitText.equals(updatedCommitText);
    }

    protected String updatedCommitText(boolean includeJobResultsInCommit) {
        return draft.toText(commitConfig, includeJobResultsInCommit).trim();
    }

    protected Perforce getLoggedInPerforceClient() {
        String reasonForFailing = reasonPerforceClientCannotBeUsed();
        if (StringUtils.isNotEmpty(reasonForFailing)) {
            throw new FatalException("Exiting as " + reasonForFailing);
        }
        return serviceLocator.getPerforce();
    }

    protected String reasonPerforceClientCannotBeUsed() {
        if (!CommandLineUtils.isCommandAvailable("p4")) {
            return "p4 command is not available";
        }
        Perforce perforce = serviceLocator.getPerforce();
        if (!perforce.isLoggedIn()) {
            return "perforce user is not logged in";
        }
        if (StringUtils.isEmpty(perforceClientConfig.perforceClientName)) {
            try {
                perforceClientConfig.perforceClientName = perforce.getClientName();
            } catch (NoPerforceClientForDirectoryException npc) {
                return npc.getMessage();
            }
        }
        return null;
    }

    protected String determineSandboxBuildNumber(String buildDisplayName) {
        Job sandboxJob = Job.buildwebJob(buildwebConfig.buildwebUrl, buildDisplayName);
        JobBuild sandboxBuild = draft.getMatchingJobBuild(sandboxJob);
        String buildId;
        if (sandboxBuild != null) {
            buildId = sandboxBuild.buildNumber();
            if (buildId == null) {
                throw new FatalException("No build number found in url " + sandboxBuild.url);
            }
        } else {
            buildId = InputUtils.readValueUntilNotBlank(buildDisplayName + " build number");
        }
        return buildId;
    }

    protected String jobWithArtifactName() {
        if (!jenkinsConfig.hasConfiguredArtifactWithoutBuildNumber()) {
            return null;
        }
        return StringUtils.isNotEmpty(jenkinsConfig.jobWithArtifact) ? jenkinsConfig.jobWithArtifact : jenkinsConfig.jobsDisplayNames[0];
    }

    private String readPendingChangelistText() {
        String changelistId = determineChangelistIdToUse();
        String changelistText = serviceLocator.getPerforce().readChangelist(changelistId);
        if (StringUtils.isEmpty(changelistText) || !changelistText.contains("\n")) {
            throw new RuntimeException("No pending changelist exists for user " + config.username);
        }
        draft.perforceChangelistId = changelistId;
        return changelistText;
    }
}
