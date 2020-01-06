package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.GitChangelistRef;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
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
        List<FileChange> gitDiffChanges = git.getChangesInDiff(gitRepoConfig.trackingBranchPath(), "head");
        log.debug("Git diff change count {}", gitDiffChanges.size());

        GitChangelistRef lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = determineRefToDiffAgainst(lastSubmittedChangelistInfo);
        String versionToSyncTo = buildwebConfig.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo.getChangelistId() : "";
        perforce.syncPerforceFiles(gitDiffChanges, versionToSyncTo);
        String diffData = git.diffTree(fromRef, "head", true, LogLevel.TRACE);

        String checkOutput = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, true);

        if (StringUtils.isNotEmpty(checkOutput)) {
            log.debug("Failed diff\n{}", diffData);
            throw new FatalException("Check of git diff failed!\n" + checkOutput);
        }

        perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, gitDiffChanges);

        String output = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, false);
        if (StringUtils.isNotEmpty(output)) {
            perforce.revertChangesInPendingChangelist(draft.perforceChangelistId);
            perforce.clean();
            throw new FatalException("Unexpected output when applying git diff!\n" + output);
        }
        log.info("Applied git diff to changelist {}", draft.perforceChangelistId);

        perforce.renameAddOrDeleteFiles(draft.perforceChangelistId, gitDiffChanges);
        log.info("Ran p4 move,add,edit,delete for affected files");
    }

    private String determineRefToDiffAgainst(GitChangelistRef lastSubmittedChangelistInfo) {
        String fromRef;
        if (buildwebConfig.syncChangelistToLatestInBranch) {
            log.info("Syncing files to be modified to changelist {} and applying diff", lastSubmittedChangelistInfo.getChangelistId());
            fromRef = lastSubmittedChangelistInfo.getCommitRef();
        } else {
            log.info("Syncing files to be modified to latest");
            fromRef = gitRepoConfig.trackingBranchPath();
        }
        return fromRef;
    }
}
