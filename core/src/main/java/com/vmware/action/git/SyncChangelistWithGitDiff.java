package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.GitChangelistRef;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.InvalidDataException;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.util.List;

@ActionDescription("Syncs the contents of the linked changelist with a git diff against the last submitted changelist in the current branch.")
public class SyncChangelistWithGitDiff extends BaseLinkedPerforceCommitUsingGitAction {
    public SyncChangelistWithGitDiff(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<FileChange> gitDiffChanges = git.getChangesInDiff(config.trackingBranchPath(), "head");
        log.debug("Git diff change count {}", gitDiffChanges.size());

        GitChangelistRef lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = determineRefToDiffAgainst(lastSubmittedChangelistInfo);
        String versionToSyncTo = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo.getChangelistId() : "";
        perforce.syncPerforceFiles(gitDiffChanges, versionToSyncTo);
        String diffData = git.diffTree(fromRef, "head", true, LogLevel.TRACE);

        String checkOutput = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, true);

        if (StringUtils.isNotBlank(checkOutput)) {
            log.debug("Failed diff\n{}", diffData);
            throw new InvalidDataException("Check of git diff failed!\n" + checkOutput);
        }

        perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, gitDiffChanges);

        String output = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, false);
        if (StringUtils.isNotBlank(output)) {
            perforce.revertChangesInPendingChangelist(draft.perforceChangelistId);
            perforce.clean();
            throw new InvalidDataException("Unexpected output when applying git diff!\n" + output);
        }
        log.info("Applied git diff to changelist {}", draft.perforceChangelistId);

        perforce.renameAddOrDeleteFiles(draft.perforceChangelistId, gitDiffChanges, versionToSyncTo);
        log.info("Ran p4 move,add,edit,delete for affected files");
    }

    private String determineRefToDiffAgainst(GitChangelistRef lastSubmittedChangelistInfo) {
        String fromRef;
        if (config.syncChangelistToLatestInBranch) {
            log.info("Syncing files to be modified to changelist {} and applying diff", lastSubmittedChangelistInfo.getChangelistId());
            fromRef = lastSubmittedChangelistInfo.getCommitRef();
        } else {
            log.info("Syncing files to be modified to latest");
            fromRef = config.trackingBranchPath();
        }
        return fromRef;
    }
}
