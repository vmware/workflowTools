/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.scm.FileChange;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommitAction extends BaseAction {

    protected ReviewRequestDraft draft;

    public BaseCommitAction(WorkflowConfig config) {
        super(config);
    }

    public void setDraft(ReviewRequestDraft draft) {
        this.draft = draft;
    }

    protected String determineChangelistIdToUse() {
        if (StringUtils.isNotBlank(draft.perforceChangelistId)) {
            return draft.perforceChangelistId;
        } else if (StringUtils.isNotBlank(perforceClientConfig.changelistId)) {
            return perforceClientConfig.changelistId;
        } else {
            return serviceLocator.getPerforce().selectPendingChangelist();
        }
    }

    protected String gitRepoOrPerforceClientCannotBeUsed() {
        String gitReason = "", perforceReason = "";
        if (git.workingDirectoryIsInGitRepo()) {
            return null; // don't need to check perforce
        } else {
            gitReason = "not in git repo";
        }
        perforceReason = perforceClientCannotBeUsed();
        if (StringUtils.isNotBlank(gitReason) && StringUtils.isNotBlank(perforceReason)) {
            return gitReason + " and " + perforceReason;
        }
        return null;
    }

    protected String readLastChange() {
        if (git.workingDirectoryIsInGitRepo()) {
            return git.lastCommitText();
        } else if (perforceClientCannotBeUsed() == null) {
            return readPendingChangelistText();
        } else {
            log.warn("Not in git repo and config value perforceClientName is not set, can't read last change");
            return "";
        }
    }

    protected List<FileChange> getFileChangesInLastCommit() {
        if (git.workingDirectoryIsInGitRepo()) {
            return git.getChangesInDiff("head~1", "head");
        } else if (perforceClientCannotBeUsed() == null) {
            String changelistId = determineChangelistIdToUse();
            return serviceLocator.getPerforce().getFileChangesForPendingChangelist(changelistId);
        } else {
            log.warn("Not in git repo and config value perforceClientName is not set, can't read last change");
            return Collections.emptyList();
        }
    }

    protected boolean commitTextHasNoChanges(boolean includeJobResultsInCommit) {
        ReviewRequestDraft existingDraft = new ReviewRequestDraft(readLastChange(), commitConfig);
        String existingCommitText = existingDraft.toText(commitConfig);
        String updatedCommitText = updatedCommitText(includeJobResultsInCommit);

        return existingCommitText.equals(updatedCommitText);
    }

    protected String updatedCommitText(boolean includeJobResultsInCommit) {
        return draft.toText(commitConfig, includeJobResultsInCommit).trim();
    }

    private String readPendingChangelistText() {
        String changelistId = determineChangelistIdToUse();
        String changelistText = serviceLocator.getPerforce().readChangelist(changelistId);
        if (StringUtils.isBlank(changelistText) || !changelistText.contains("\n")) {
            throw new RuntimeException("No pending changelist exists for user " + config.username);
        }
        draft.perforceChangelistId = changelistId;
        return changelistText;
    }
}
